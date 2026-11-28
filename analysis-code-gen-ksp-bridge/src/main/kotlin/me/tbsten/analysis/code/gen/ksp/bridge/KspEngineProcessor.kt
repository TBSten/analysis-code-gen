@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.ksp.bridge

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiJavaFile
import me.tbsten.analysis.code.gen.engine.spi.EngineFinishContext
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessor
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysis.code.gen.engine.spi.EngineRoundContext
import me.tbsten.analysis.code.gen.ksp.bridge.common.AnyChanges
import me.tbsten.analysis.code.gen.ksp.bridge.common.KSObjectCacheManager
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.AAResolutionStrategy
import me.tbsten.analysis.code.gen.ksp.bridge.common.impl.PsiResolutionStrategy
import me.tbsten.analysis.code.gen.ksp.bridge.impl.IncrementalContextAA
import me.tbsten.analysis.code.gen.ksp.bridge.impl.ResolverAAImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.Deferrable
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.KSFileImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.KSFileJavaImpl
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.Restorable
import me.tbsten.analysis.code.gen.ksp.bridge.impl.symbol.kotlin.analyze
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * KSP の [SymbolProcessor] 1 つを、エンジンのラウンドで回す（ADR-017、ADR-062）。
 *
 * KSP2 の `KotlinSymbolProcessing.execute()` のラウンドのループ（google/ksp の
 * kotlin-analysis-api/src/main/kotlin/com/google/devtools/ksp/impl/KotlinSymbolProcessing.kt）を参考に書き直したもの。
 * ラウンドを回す・AA のキャッシュを捨てる・生成物を次のラウンドの入力にするのはエンジンの役目で、
 * ここは 1 ラウンド分の Resolver の組み立てと、defer されたシンボルの持ち越しだけを受け持つ。
 */
internal class KspEngineProcessor(
    private val provider: SymbolProcessorProvider,
    private val environment: EngineProcessorEnvironment,
) : EngineProcessor {
    override val name: String = provider::class.java.name

    private val codeGenerator = BridgeCodeGenerator()
    private val logger = BridgeKspLogger(environment.logger)
    private val kspConfig = bridgeKspConfig(environment)

    // 増分（Lookup の記録）は M2（ADR-054）。M1 は記録しない IncrementalContextAA を一時ディレクトリで動かす
    private val cachesDir: File = kotlin.io.path.createTempDirectory("acg-ksp-bridge").toFile()
    private val incrementalContext: IncrementalContextAA by lazy {
        val anyChanges = AnyChanges(cachesDir)
        IncrementalContextAA(
            false,
            LookupTracker.DO_NOTHING,
            File(anyChanges.filePath).relativeTo(cachesDir),
            kspConfig.incrementalContextLoggingOptions,
            cachesDir,
            File(cachesDir, "caches"),
            File(cachesDir, "out"),
            emptyList(),
            emptyList(),
            emptyList(),
        )
    }

    private var processor: SymbolProcessor? = null
    private val processorsRegisteredForNewFeatures = mutableSetOf<SymbolProcessor>()

    // 前のラウンドで defer されたシンボル。AA のキャッシュが捨てられても復元できるよう pointer で持つ
    private var deferred: List<Restorable> = emptyList()

    // finish() でも最後のラウンドの KS オブジェクトを使えるよう、最後に組み立てた状態を覚えておく
    private var lastModule: KaSourceModule? = null
    private var lastResolver: ResolverAAImpl? = null

    override fun process(context: EngineRoundContext) {
        val round = context.native
        // 前のラウンドの KS オブジェクトは、エンジンが AA のキャッシュを捨てたので無効。ラウンドの頭で捨てる
        KSObjectCacheManager.clear()
        enterState(context.analysis.sourceModule, resolver = null)
        run {
            codeGenerator.begin(context.output, round.allFiles + round.allJavaFiles)
            val processor = processor ?: createProcessor().also { processor = it }

            val allKSFiles = toKSFiles(round.allFiles, round.allJavaFiles)
            val newKSFiles = toKSFiles(round.newFiles, if (round.round == 1) round.allJavaFiles else emptyList())
            val deferredSymbols = if (deferred.isEmpty()) emptyMap() else mapOf(processor to deferred)
            val resolutionStrategy =
                if (kspConfig.experimentalPsiResolution) {
                    PsiResolutionStrategy(newKSFiles, deferredSymbols)
                } else {
                    AAResolutionStrategy(newKSFiles, deferredSymbols)
                }
            val resolver =
                ResolverAAImpl(
                    allKSFiles,
                    context.analysis.project,
                    incrementalContext,
                    resolutionStrategy,
                    processorsRegisteredForNewFeatures,
                )
            enterState(context.analysis.sourceModule, resolver)
            resolver.functionAsMemberOfCache = mutableMapOf()
            resolver.propertyAsMemberOfCache = mutableMapOf()
            resolver.currentProcessor = processor

            deferred =
                runProcessor {
                    processor
                        .process(resolver)
                        .filter { it.origin == Origin.KOTLIN || it.origin == Origin.JAVA }
                        .filterIsInstance<Deferrable>()
                        .mapNotNull(Deferrable::defer)
                }
        }
    }

    override fun finish(context: EngineFinishContext) {
        val processor = processor ?: return
        lastModule?.let { enterState(it, lastResolver) }
        // finish 中の生成を許す（ADR-017）。生成物は次のラウンドの入力にならない
        codeGenerator.begin(context.output, codeGenerator.knownFiles)
        try {
            runProcessor { processor.finish() }
        } finally {
            cleanUp()
        }
    }

    override fun onError() {
        try {
            lastModule?.let { enterState(it, lastResolver) }
            processor?.let { runProcessor { it.onError() } }
        } finally {
            cleanUp()
        }
    }

    private fun enterState(
        module: KaSourceModule,
        resolver: ResolverAAImpl?,
    ) {
        ResolverAAImpl.ktModule = module
        ResolverAAImpl.kspConfig = kspConfig
        resolver?.let { ResolverAAImpl.instance = it }
        lastModule = module
        lastResolver = resolver ?: lastResolver
    }

    private fun createProcessor(): SymbolProcessor {
        val kotlinVersion = environment.kotlinVersion.toKotlinVersion()
        val symbolProcessorEnvironment =
            SymbolProcessorEnvironment(
                environment.options,
                kotlinVersion,
                codeGenerator,
                logger,
                kotlinVersion,
                kotlinVersion,
                ResolverAAImpl.ktModule.targetPlatform.toPlatformInfo(kspConfig),
                KotlinVersion(2, 0),
                registerProcessorForNewFeatures = processorsRegisteredForNewFeatures::add,
            )
        return runProcessor { provider.create(symbolProcessorEnvironment) }
    }

    private fun toKSFiles(
        ktFiles: List<KtFile>,
        javaFiles: List<PsiJavaFile>,
    ): List<KSFile> = ktFiles.map { analyze { KSFileImpl.getCached(it.symbol) } } + javaFiles.map { KSFileJavaImpl.getCached(it) }

    /** KSP の内部実装に依存する Processor を、原因の分かる診断にする（ADR-035）。 */
    private fun <T> runProcessor(block: () -> T): T =
        try {
            block()
        } catch (e: LinkageError) {
            throw kspInternalDependencyError(e) ?: e
        } catch (e: ClassNotFoundException) {
            throw kspInternalDependencyError(e) ?: e
        } catch (e: ClassCastException) {
            throw kspInternalDependencyError(e) ?: e
        }

    private fun kspInternalDependencyError(cause: Throwable): Throwable? {
        val missing = cause.message ?: return null
        val internal = KSP_INTERNAL_PACKAGES.any { missing.replace('/', '.').contains(it) }
        if (!internal) return null
        return IllegalStateException(
            "KSP Processor '$name' は KSP の内部実装（公開 API 外）に依存しているため Bridge では動きません: $missing",
            cause,
        )
    }

    private fun cleanUp() {
        deferred = emptyList()
        lastModule = null
        lastResolver = null
        // 移植コードのスレッドごとの状態を外す（同じスレッドで別の実行単位が動いても混ざらないように）
        KSObjectCacheManager.clear()
        ResolverAAImpl.tearDown()
        runCatching { incrementalContext.closeFiles() }
        cachesDir.deleteRecursively()
    }

    private companion object {
        val KSP_INTERNAL_PACKAGES = listOf("com.google.devtools.ksp.impl.", "com.google.devtools.ksp.common.")
    }
}
