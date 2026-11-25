package me.tbsten.analysiscodegen.differential

import me.tbsten.analysiscodegen.testing.CodegenDiagnostic
import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.TestEngine
import me.tbsten.analysiscodegen.testing.runCodegenTest
import java.io.File
import java.nio.file.Files

/**
 * 1 つの実行系で入力を動かして [EngineRun] を返す（ADR-023）。
 *
 * 決まり:
 * - 動かせないときは例外ではなく [EngineRun.Unavailable] を返す（未実装・その Processor の版が無い）
 * - 生成物のキーは `kotlin/…`・`java/…`・`resources/…`、診断の位置は「入力のソースルートからの相対パス:行」か、
 *   作業ディレクトリ（[RunOutput.roots]）の下の絶対パスで返す。正規化（[Normalizer]）が同じ形に揃える
 * - Processor の Logger の出力は [DiagnosticRecord.fromProcessor] = true で返す
 * - ハーネスの不具合（実行系の外の例外）はそのまま投げる。入力のせいで失敗したビルドは [RunOutput.success] = false
 */
public interface EngineRunner {
    public val engine: EngineKind

    public fun run(input: DiffInput): EngineRun
}

/**
 * K: 公式 KSP2 で動かす（kct fork 経由、同じプロセス内）。[cache] があれば同じ入力は 2 回目から読むだけ。
 * [execute] はテストで差し替える口（キャッシュが効いて実際には動かないことを確かめる、XD-17）。
 */
public class KspReferenceRunner(
    private val cache: KResultStore? = null,
    private val versions: EngineVersions = EngineVersions.current(),
    private val execute: (DiffInput) -> RunOutput = ::runKsp2,
) : EngineRunner {
    override val engine: EngineKind = EngineKind.K

    override fun run(input: DiffInput): EngineRun {
        if (input.processors.any { it.ksp == null }) {
            return EngineRun.Unavailable(engine, "KSP 版の無い Processor がある: ${input.processors.filter { it.ksp == null }.map { it.id }}")
        }
        if (input.sources.any { it.sourceSet != DiffSource.MAIN }) {
            // KMP の SourceSet は Gradle フィクスチャ経由のランナー（XD-18、ADR-023 の後続）で扱う
            return EngineRun.Unavailable(engine, "同じプロセス内の K は SourceSet main だけに対応する")
        }
        val start = System.nanoTime()
        val key = cache?.let { KCacheKey.of(input, versions) }
        key?.let { cache?.get(it) }?.let { hit ->
            return EngineRun.Completed(engine, hit, RunMetrics(elapsedMillis(start), cacheHit = true))
        }
        val output = execute(input)
        if (key != null) cache?.put(key, output)
        return EngineRun.Completed(engine, output, RunMetrics(elapsedMillis(start)))
    }
}

/**
 * B: analysisCodeGen + Bridge で元の KSP Processor を動かす。エンジンは P2 で実装するので、
 * それまでは [EngineRun.Unavailable] を返す（[TestEngine.AnalysisCodegen] が [NotImplementedError] を投げる間）。
 */
public class BridgeRunner : EngineRunner {
    override val engine: EngineKind = EngineKind.B

    override fun run(input: DiffInput): EngineRun {
        if (input.processors.any { it.ksp == null }) {
            return EngineRun.Unavailable(engine, "KSP 版の無い Processor がある")
        }
        val start = System.nanoTime()
        val recorder = ProcessorRecorder()
        val workingDir = Files.createTempDirectory("acg-diff-b").toFile()
        val result =
            try {
                runCodegenTest(TestEngine.AnalysisCodegen) { configure(input, workingDir, recorder) }
            } catch (e: NotImplementedError) {
                return EngineRun.Unavailable(engine, "analysisCodeGen のエンジンが未実装（P2）: ${e.message}")
            }
        return EngineRun.Completed(engine, result.toRunOutput(workingDir, recorder), RunMetrics(elapsedMillis(start)))
    }
}

/**
 * N: analysisCodeGen + Native 移植版。analysis-code-gen-processor-api（ACG-34）と Native の TestProcessor（ADR-021）が入るまでは
 * いつも [EngineRun.Unavailable]。入ったら [DiffProcessor.nativeProviderClass] を読み込んで動かす。
 */
public class NativeRunner : EngineRunner {
    override val engine: EngineKind = EngineKind.N

    override fun run(input: DiffInput): EngineRun {
        val missing = input.processors.filter { it.nativeProviderClass == null }.map { it.id }
        if (missing.isNotEmpty()) return EngineRun.Unavailable(engine, "Native 版の無い Processor がある: $missing")
        return EngineRun.Unavailable(engine, "Native Processor を testing から動かす口が未実装（analysis-code-gen-processor-api 待ち、ADR-021）")
    }
}

/** K の実体: KSP2 で 1 回動かし、Logger とラウンドを記録して [RunOutput] にする。 */
public fun runKsp2(input: DiffInput): RunOutput {
    val workingDir = Files.createTempDirectory("acg-diff-k").toFile()
    val recorder = ProcessorRecorder()
    return try {
        runCodegenTest(TestEngine.Ksp2) { configure(input, workingDir, recorder) }.toRunOutput(workingDir, recorder)
    } finally {
        workingDir.deleteRecursively()
    }
}

private fun me.tbsten.analysiscodegen.testing.CodegenTestSpec.configure(
    input: DiffInput,
    workingDir: File,
    recorder: ProcessorRecorder,
) {
    this.workingDir = workingDir
    input.sources.forEach { s ->
        sourceSet(s.sourceSet) {
            if (s.path.endsWith(".java")) java(s.path, s.content) else kotlin(s.path, s.content)
        }
    }
    // 毎回新しい Provider を作り、記録用に包む
    input.processors.forEach { p -> ksp(recorder.wrap(checkNotNull(p.ksp).invoke())) }
    options.putAll(input.options)
    classpath.addAll(input.classpath)
    languageVersion = input.languageVersion
}

// KSP2 が Logger のエラーに付け足す決まり文句。B / N には無いので比べない
private const val KSP_ERROR_BANNER = "Error occurred in KSP, check log for detail"

private fun CodegenTestResult.toRunOutput(
    workingDir: File,
    recorder: ProcessorRecorder,
): RunOutput {
    val logs = recorder.logs()
    // kct の診断には Logger の出力も（位置を文言に繋げた形で）入っているので、記録した Logger の出力を 1 件ずつ取り除いて
    // コンパイラの診断だけを残す
    val pending = logs.map { it.kctForm() }.toMutableList()
    val compiler =
        diagnostics.filter { d ->
            if (d.message == KSP_ERROR_BANNER) return@filter false
            val i = pending.indexOf(d.message)
            if (i >= 0) {
                pending.removeAt(i)
                false
            } else {
                true
            }
        }
    val generatedRoot = workingDir.resolve("ksp/sources")
    val rounds =
        recorder.rounds().mapIndexed { i, files ->
            RoundRecord(i + 1, files.map { f -> f.relativeToOrSelf(generatedRoot).invariantSeparatorsPath })
        }
    return RunOutput(
        success = isSuccess,
        files = generatedFiles,
        diagnostics = compiler.map { it.toRecord() } + logs,
        rounds = rounds,
        roots = listOf(workingDir.absolutePath, workingDir.canonicalPath).distinct(),
    )
}

private fun DiagnosticRecord.kctForm(): String = if (location != null) "$location: $message" else message

private fun CodegenDiagnostic.toRecord() =
    DiagnosticRecord(
        severity =
            when (severity) {
                CodegenDiagnostic.Severity.Error -> DiagnosticRecord.Severity.Error
                CodegenDiagnostic.Severity.Warning -> DiagnosticRecord.Severity.Warning
                CodegenDiagnostic.Severity.Info -> DiagnosticRecord.Severity.Info
                CodegenDiagnostic.Severity.Logging -> DiagnosticRecord.Severity.Logging
            },
        message = message,
    )

private fun elapsedMillis(start: Long) = (System.nanoTime() - start) / 1_000_000
