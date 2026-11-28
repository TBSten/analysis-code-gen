@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironment
import me.tbsten.analysis.code.gen.analysis.ModuleId
import me.tbsten.analysis.code.gen.engine.spi.AnalysisAccess
import me.tbsten.analysis.code.gen.engine.spi.DuplicatePolicy
import me.tbsten.analysis.code.gen.engine.spi.EngineFinishContext
import me.tbsten.analysis.code.gen.engine.spi.EngineLogger
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessorEnvironment
import me.tbsten.analysis.code.gen.engine.spi.EngineRoundContext
import me.tbsten.analysis.code.gen.engine.spi.OutputKind
import me.tbsten.analysis.code.gen.engine.spi.OutputSink
import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysis.code.gen.processor.CodeGenerator
import me.tbsten.analysis.code.gen.processor.CodegenLogger
import me.tbsten.analysis.code.gen.processor.CompilationContext
import me.tbsten.analysis.code.gen.processor.DeferredSymbol
import me.tbsten.analysis.code.gen.processor.Dependencies
import me.tbsten.analysis.code.gen.processor.ExecutionUnit
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.processor.SourceSetContext
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class SourceSetUnitImpl(
    override val name: String,
) : ExecutionUnit.SourceSetUnit {
    override fun toString(): String = "SourceSetUnit($name)"
}

internal class CompilationUnitImpl(
    override val name: String,
) : ExecutionUnit.CompilationUnit {
    override fun toString(): String = "CompilationUnit($name)"
}

/**
 * 1 ラウンドの間だけ有効な SourceSet の見え方。ファイルの一覧はラウンドの頭で 1 回だけ取り、ラウンド中は固定する（RND-03）。
 */
internal class SourceSetContextImpl(
    override val name: String,
    private val snapshot: RoundFiles,
) : SourceSetContext {
    override val module: KaSourceModule by lazy { snapshot.env.sourceModule(ModuleId(name)) }
    override val platform: TargetPlatform get() = module.targetPlatform
    override val ownFiles: List<KtFile> by lazy { snapshot.ownKotlin(name) }
    override val visibleFiles: List<KtFile> by lazy { snapshot.visibleKotlin(name) }
    override val dependencies: List<SourceSetContext> by lazy { snapshot.dependsOn(name).map { snapshot.sourceSetContext(it) } }

    override fun toString(): String = "SourceSetContext($name)"
}

internal class CompilationContextImpl(
    override val targetName: String,
    override val compilationName: String,
    override val sourceSets: List<SourceSetContext>,
    override val defaultSourceSet: SourceSetContext,
) : CompilationContext {
    override val platform: TargetPlatform get() = defaultSourceSet.platform
}

/** ある Processor の、あるラウンドの RoundContext。ラウンドが終わったら [close] し、以後の生成・defer は拒否する。 */
internal class RoundContextImpl(
    override val round: Int,
    override val executionUnit: ExecutionUnit,
    override val sourceSet: SourceSetContext,
    override val compilation: CompilationContext?,
    private val files: RoundFiles,
    override val logger: CodegenLogger,
    override val options: Map<String, String>,
    override val deferred: List<DeferredSymbol>,
    private val sink: ProcessorOutputSink,
) : AnalysisCodegenRoundContext {
    @Volatile private var closed = false
    private val deferredOut = mutableListOf<DeferredSymbol>()

    /** このラウンドで defer されたもの。 */
    val deferredByProcessor: List<DeferredSymbol> get() = synchronized(deferredOut) { deferredOut.toList() }

    override val allFiles: List<KtFile> get() = files.allKotlin
    override val allJavaFiles: List<PsiJavaFile> get() = files.allJava
    override val newFiles: List<KtFile> get() = files.newKotlin
    override val changedFiles: List<KtFile> get() = emptyList()
    override val removedFilePaths: Set<String> get() = emptySet()

    override val codeGenerator: CodeGenerator =
        object : CodeGenerator {
            override fun generateKotlin(
                packageName: String,
                fileName: String,
                dependencies: Dependencies,
                content: String,
            ) = writeSource(OutputKind.Kotlin, packageName, fileName, "kt", dependencies, content)

            override fun generateJava(
                packageName: String,
                fileName: String,
                dependencies: Dependencies,
                content: String,
            ) = writeSource(OutputKind.Java, packageName, fileName, "java", dependencies, content)

            override fun generateResource(
                relativePath: String,
                dependencies: Dependencies,
                content: ByteArray,
            ) {
                checkOpen()
                sink.write(OutputKind.Resource, relativePath, content, dependencies, DuplicatePolicy.Native)
            }
        }

    private fun writeSource(
        kind: OutputKind,
        packageName: String,
        fileName: String,
        extension: String,
        dependencies: Dependencies,
        content: String,
    ) {
        checkOpen()
        require(packageName.isEmpty() || packageName.split('.').all { it.isJavaIdentifier() }) { "パッケージ名が不正: '$packageName'" }
        require(fileName.isNotEmpty() && '/' !in fileName && '\\' !in fileName && fileName != "." && fileName != "..") {
            "ファイル名が不正: '$fileName'"
        }
        val dir = packageName.replace('.', '/')
        val path = if (dir.isEmpty()) "$fileName.$extension" else "$dir/$fileName.$extension"
        // 文字コードは UTF-8、改行は LF に固定する（API-13）
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        sink.write(kind, path, normalized.toByteArray(Charsets.UTF_8), dependencies, DuplicatePolicy.Native)
    }

    override fun defer(
        pointer: KaSymbolPointer<KaSymbol>,
        payload: String?,
    ) {
        checkOpen()
        synchronized(deferredOut) { deferredOut += DeferredSymbol(pointer, payload) }
    }

    fun close() {
        closed = true
    }

    private fun checkOpen() = check(!closed) { "ラウンド $round は終わっている。finish / onError の中や、ラウンドの後では生成・defer できない（ADR-013）" }

    private fun String.isJavaIdentifier(): Boolean =
        isNotEmpty() && Character.isJavaIdentifierStart(this[0]) && drop(1).all { Character.isJavaIdentifierPart(it) }
}

/** Processor に紐づいた出力口。[open] が false の間は書けない。 */
internal class ProcessorOutputSink(
    private val processorName: String,
    private val round: Int,
    private val outputs: OutputTransaction,
) : OutputSink {
    override fun write(
        kind: OutputKind,
        relativePath: String,
        content: ByteArray,
        dependencies: Dependencies,
        duplicatePolicy: DuplicatePolicy,
    ) = outputs.write(processorName, round, kind, relativePath, content, dependencies, duplicatePolicy)

    override fun associate(
        kind: OutputKind,
        relativePath: String,
        dependencies: Dependencies,
    ) {
        // 依存の追加は増分（M2、ADR-015）で使う。M1 は全件処理なので記録しない
        OutputTransaction.checkRelativePath(relativePath)
    }
}

internal class EngineRoundContextImpl(
    override val native: AnalysisCodegenRoundContext,
    override val output: OutputSink,
    override val analysis: AnalysisAccess,
) : EngineRoundContext

internal class EngineFinishContextImpl(
    override val output: OutputSink,
) : EngineFinishContext

internal class AnalysisAccessImpl(
    private val env: AnalysisEnvironment,
    private val owner: ModuleId,
) : AnalysisAccess {
    override val project: Project get() = env.project
    override val sourceModule: KaSourceModule get() = env.sourceModule(owner)
    override val libraryModules: List<KaLibraryModule> get() = env.libraryModules(owner)

    override fun <R> analyze(
        element: KtElement,
        block: KaSession.() -> R,
    ): R = analyze(element) { block() }
}

internal class EngineProcessorEnvironmentImpl(
    override val executionUnit: ExecutionUnit,
    override val options: Map<String, String>,
    override val kotlinVersion: String,
    override val frameworkVersion: String,
    override val analysis: AnalysisAccess,
    val collector: DiagnosticCollector,
    componentName: String,
) : EngineProcessorEnvironment {
    override val logger: EngineLogger = EngineLoggerImpl(collector, componentName)
}
