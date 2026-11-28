@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironment
import me.tbsten.analysis.code.gen.analysis.ModuleId
import me.tbsten.analysis.code.gen.analysis.SourceChangeSet
import me.tbsten.analysis.code.gen.analysis.SourceFileId
import me.tbsten.analysis.code.gen.engine.OutputTransaction.Companion.dirName
import me.tbsten.analysis.code.gen.engine.spi.EngineProcessor
import me.tbsten.analysis.code.gen.engine.spi.OutputKind
import me.tbsten.analysis.code.gen.engine.spi.ProcessorComponent
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.UnitKind
import me.tbsten.analysis.code.gen.processor.DeferredSymbol
import me.tbsten.analysis.code.gen.processor.ExecutionUnit
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import java.io.File

/**
 * 1 実行単位の 1 回の実行（ADR-013・014）。
 *
 * Processor の作成 → ラウンド（全 Processor の process → Validate → Round Commit → 解析環境へ反映）を新しいファイルが
 * 無くなるまで繰り返す → 残った deferred の判定 → finish → Build Commit。どこかで error が出たら onError を呼び、出力は前回のまま。
 */
internal class UnitRun(
    private val components: EngineComponents,
    private val model: ProjectModel,
    private val unit: ExecutionUnitModel,
    private val extraComponents: List<ProcessorComponent>,
) {
    private val rootDir = File(model.rootDir)
    private val frameworkVersion: String =
        UnitRun::class.java.`package`?.implementationVersion ?: "0.0.0-dev"
    private val diagnostics = DiagnosticCollector(rootDir, frameworkVersion)
    private val outputs = OutputTransaction(rootDir, unit, diagnostics)

    private val compilationModel = model.compilations.firstOrNull { it.name == unit.target && unit.kind == UnitKind.Compilation }

    /** 生成物の所有者となる SourceSet（Compilation 単位では default SourceSet。ADR-013）。 */
    private val owner: String = compilationModel?.defaultSourceSet ?: unit.target

    private val executionUnit: ExecutionUnit =
        if (unit.kind == UnitKind.Compilation) CompilationUnitImpl(unit.name) else SourceSetUnitImpl(unit.name)

    /** Processor に渡すオプション。`analysisCodeGen.` で始まる予約キーは渡さない（ADR-013）。 */
    private val processorOptions: Map<String, String> = unit.options.filterKeys { !it.startsWith(RESERVED_PREFIX) }

    private var rounds = 0

    /** 作れた Processor（失敗時に onError を呼ぶ相手）。 */
    private val processors = mutableListOf<EngineProcessor>()

    fun run(): EngineResult {
        outputs.prepare()
        val env =
            try {
                components.analysisEnvironmentFactory.create(model, unit.name)
            } catch (t: Throwable) {
                diagnostics.exception(null, "解析環境の作成", t)
                return fail()
            }
        return try {
            env.use { runWith(it) }
        } catch (t: Throwable) {
            // エンジン自身の不具合。Processor の例外は個別に捕まえているので、ここに来るのは想定外のもの
            diagnostics.exception(null, "analysisCodeGen のエンジン", t)
            fail()
        }
    }

    private fun runWith(env: AnalysisEnvironment): EngineResult {
        val ownerModule = ModuleId(owner)
        val access = AnalysisAccessImpl(env, ownerModule)
        createProcessors(access)
        if (diagnostics.hasErrors) return fail()
        val ordered = orderProcessors(processors.toList())

        val roots = RoundFiles.rootsOf(model, OutputKind.entries.map { outputs.stagingDir(it).relativeTo(rootDir).invariantSeparatorsPath })
        val maxRounds = model.analysis.maxRounds
        var previousGenerated: Set<String>? = null
        var pending: Map<EngineProcessor, List<DeferredSymbol>> = emptyMap()

        while (true) {
            rounds++
            val restored = restoreDeferred(env, pending)
            val files = RoundFiles(env, model, unit, owner, roots, previousGenerated)
            val sourceSet = files.sourceSetContext(owner)
            val compilation =
                compilationModel?.let { c ->
                    CompilationContextImpl(c.targetName, c.compilationName, c.sourceSets.map(files::sourceSetContext), sourceSet)
                }
            val defers = linkedMapOf<EngineProcessor, List<DeferredSymbol>>()
            for (processor in ordered) {
                val sink = ProcessorOutputSink(processor.name, rounds, outputs)
                val native =
                    RoundContextImpl(
                        round = rounds,
                        executionUnit = executionUnit,
                        sourceSet = sourceSet,
                        compilation = compilation,
                        files = files,
                        logger = CodegenLoggerImpl(diagnostics, processor.name),
                        options = processorOptions,
                        deferred = restored[processor].orEmpty(),
                        sink = sink,
                    )
                try {
                    processor.process(EngineRoundContextImpl(native, sink, access))
                } catch (t: Throwable) {
                    diagnostics.exception(processor.name, "ラウンド $rounds の process", t)
                } finally {
                    native.close()
                }
                native.deferredByProcessor.takeIf { it.isNotEmpty() }?.let { defers[processor] = it }
            }
            val commit = outputs.validateAndCommit()
            if (commit == null || diagnostics.hasErrors) return fail()
            pending = defers

            val newSources = commit.added.filter { it.kind != OutputKind.Resource }
            // 新しいパスのファイルが増えたときだけ次のラウンドに進む（ADR-014）
            if (newSources.isEmpty()) break
            if (rounds >= maxRounds) {
                reportMaxRounds(env, maxRounds, commit, pending)
                return fail()
            }
            env.applyChanges(
                SourceChangeSet(
                    module = ownerModule,
                    added = newSources.associate { SourceFileId(outputs.stagingPath(it)) to it.content.toString(Charsets.UTF_8) },
                    modified = emptyMap(),
                    removed = emptySet(),
                ),
            )
            previousGenerated = newSources.filter { it.kind == OutputKind.Kotlin }.map { outputs.stagingPath(it) }.toSet()
        }

        reportUnresolvedDeferred(env, pending)
        if (diagnostics.hasErrors) return fail()

        for (processor in ordered) {
            try {
                processor.finish(EngineFinishContextImpl(ProcessorOutputSink(processor.name, rounds, outputs)))
            } catch (t: Throwable) {
                diagnostics.exception(processor.name, "finish", t)
            }
        }
        // finish 中の生成（Bridge の KSP 互換だけ）は解析対象にせず、出力にだけ入れる（ADR-017）
        if (outputs.validateAndCommit() == null || diagnostics.hasErrors) {
            outputs.discard()
            return result(success = false, generated = emptyList())
        }
        return result(success = true, generated = outputs.buildCommit())
    }

    /** component ごとに Frontend を選んで Processor を作る（ADR-017 の Provider の選択）。 */
    private fun createProcessors(access: AnalysisAccessImpl) {
        val all =
            unit.processors.map { p ->
                ClasspathProcessorComponent(p.component, p.classpath.map { resolve(it) })
            } + extraComponents
        for (component in all) {
            val handlers =
                components.frontends.filter { frontend ->
                    runCatching { frontend.canHandle(component) }
                        .onFailure { diagnostics.exception(component.coordinates, "${frontend.id} の Provider の判定", it) }
                        .getOrDefault(false)
                }
            if (handlers.isEmpty()) {
                if (component.hasService(KSP_SERVICE)) {
                    diagnostics.report(
                        Diagnostic.Severity.Error,
                        "${component.coordinates} は KSP の Processor だが、KSP Bridge（analysis-code-gen-ksp-bridge）が見つからない",
                        component.coordinates,
                    )
                } else {
                    diagnostics.report(
                        Diagnostic.Severity.Warning,
                        "${component.coordinates} に Processor の Provider が無いので無視する",
                        component.coordinates,
                    )
                }
                continue
            }
            val chosen = handlers.first()
            if (chosen.id == NativeProcessorFrontend.ID && component.hasService(KSP_SERVICE)) {
                val names = (component as? ClasspathProcessorComponent)?.serviceClassNames(KSP_SERVICE)?.joinToString(", ") ?: KSP_SERVICE
                diagnostics.report(
                    Diagnostic.Severity.Info,
                    "${component.coordinates} には Native の Provider があるので、KSP の Provider（$names）は使わない（ADR-017）",
                    component.coordinates,
                )
            }
            val environment =
                EngineProcessorEnvironmentImpl(
                    executionUnit = executionUnit,
                    options = processorOptions,
                    kotlinVersion = model.analysis.analysisApiVersion,
                    frameworkVersion = frameworkVersion,
                    analysis = access,
                    collector = diagnostics,
                    componentName = component.coordinates,
                )
            try {
                processors += chosen.create(component, environment)
            } catch (f: ProcessorFailure) {
                diagnostics.exception(f.processorName, f.phase, f)
            } catch (t: Throwable) {
                diagnostics.exception(component.coordinates, "Processor の作成", t)
            }
        }
    }

    /** 登録順。`analysisCodeGen.internal.shuffleSeed` があれば順番に依存していないかを確かめるため混ぜる（DET-02）。 */
    private fun orderProcessors(list: List<EngineProcessor>): List<EngineProcessor> {
        val seed = unit.options[SHUFFLE_SEED]?.toLongOrNull() ?: return list
        return list.shuffled(kotlin.random.Random(seed))
    }

    /** 前のラウンドの deferred を新しいセッションで復元する。復元できないものは落として warning（ADR-014、DEF-03）。 */
    private fun restoreDeferred(
        env: AnalysisEnvironment,
        pending: Map<EngineProcessor, List<DeferredSymbol>>,
    ): Map<EngineProcessor, List<DeferredSymbol>> {
        if (pending.isEmpty()) return emptyMap()
        val module = env.sourceModule(ModuleId(owner))
        return pending.mapValues { (processor, list) ->
            list.filter { d ->
                val ok = runCatching { analyze(module) { d.pointer.restoreSymbol() != null } }.getOrDefault(false)
                if (!ok) {
                    diagnostics.report(
                        Diagnostic.Severity.Warning,
                        "defer したシンボルを復元できないので落とす（元の宣言が消えた可能性）: ${d.pointer}${d.payload?.let { "（payload: $it）" } ?: ""}",
                        processor.name,
                    )
                }
                ok
            }
        }
    }

    /** deferred を「Processor: FQN（payload）」の行にする。 */
    private fun describeDeferred(
        env: AnalysisEnvironment,
        pending: Map<EngineProcessor, List<DeferredSymbol>>,
    ): List<String> {
        val module = runCatching { env.sourceModule(ModuleId(owner)) }.getOrNull()
        return pending
            .flatMap { (processor, list) ->
                list.map { d ->
                    val name =
                        module?.let { m -> runCatching { analyze(m) { d.pointer.restoreSymbol()?.let(::symbolName) } }.getOrNull() }
                            ?: d.pointer.toString()
                    "${processor.name}: $name${d.payload?.let { "（payload: $it）" } ?: ""}"
                }
            }.sorted()
    }

    private fun symbolName(symbol: KaSymbol): String =
        (symbol as? KaClassLikeSymbol)?.classId?.asFqNameString()
            ?: (symbol as? KaCallableSymbol)?.callableId?.toString()
            ?: symbol.toString()

    private fun reportMaxRounds(
        env: AnalysisEnvironment,
        maxRounds: Int,
        commit: OutputTransaction.RoundCommit,
        pending: Map<EngineProcessor, List<DeferredSymbol>>,
    ) {
        val generated = commit.added.joinToString("\n") { "  ${it.processorName}: ${it.kind.dirName}/${it.relativePath}" }
        val deferred = describeDeferred(env, pending)
        val deferredText = if (deferred.isEmpty()) "" else "\n残っている deferred:\n" + deferred.joinToString("\n") { "  $it" }
        diagnostics.report(
            Diagnostic.Severity.Error,
            "ラウンド数が上限（maxRounds = $maxRounds）に達しても新しいファイルが生成され続けている。" +
                "ラウンド $rounds で生成したもの:\n$generated$deferredText",
            commit.added.firstOrNull()?.processorName,
        )
    }

    /** 新しいファイルが無いのに deferred が残った（ADR-014、DEF-04）。既定は error、設定で warning。 */
    private fun reportUnresolvedDeferred(
        env: AnalysisEnvironment,
        pending: Map<EngineProcessor, List<DeferredSymbol>>,
    ) {
        if (pending.values.all { it.isEmpty() }) return
        val severity =
            if (model.analysis.unresolvedDeferred == DeferredSeverityModel.Error) Diagnostic.Severity.Error else Diagnostic.Severity.Warning
        val lines = describeDeferred(env, pending)
        diagnostics.report(
            severity,
            "最終ラウンド（$rounds）の後も解決できない deferred が残った:\n" + lines.joinToString("\n") { "  $it" },
            pending.keys.singleOrNull()?.name,
        )
    }

    private fun fail(): EngineResult {
        for (processor in processors) {
            try {
                processor.onError()
            } catch (t: Throwable) {
                diagnostics.exception(processor.name, "onError", t)
            }
        }
        outputs.discard()
        return result(success = false, generated = emptyList())
    }

    private fun result(
        success: Boolean,
        generated: List<String>,
    ) = EngineResult(success, rounds, diagnostics.diagnostics, generated)

    private fun resolve(path: String): File = File(path).let { if (it.isAbsolute) it else File(rootDir, path) }

    companion object {
        const val RESERVED_PREFIX: String = "analysisCodeGen."
        const val SHUFFLE_SEED: String = "analysisCodeGen.internal.shuffleSeed"
        const val KSP_SERVICE: String = "com.google.devtools.ksp.processing.SymbolProcessorProvider"
    }
}
