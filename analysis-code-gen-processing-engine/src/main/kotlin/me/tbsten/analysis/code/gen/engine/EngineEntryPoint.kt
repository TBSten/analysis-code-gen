@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.engine

import kotlinx.serialization.json.Json
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.util.function.BiConsumer

/**
 * Gradle の Worker から呼ぶ入口（ADR-063）。
 *
 * gradle-plugin はエンジンを import できない（ADR-012、GRD-21）ので、Worker の隔離 ClassLoader からリフレクションでこれを呼ぶ。
 * ClassLoader をまたいでも型がずれないよう、引数と戻り値は JDK の型だけにする。
 */
@InternalAnalysisCodegenApi
public object EngineEntryPoint {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * [modelJson]（ProjectModel の JSON）の実行単位 [unitName] を処理する。
     *
     * [log] には (severity, 行) を渡す。severity は `error` / `warning` / `info` / `lifecycle`。
     * 行は `[<processor>] <file>:<line>: <message>`（CLI-03 と同じ形から severity を除いたもの）。Gradle 側が `e: ` などを付ける。
     *
     * @return 成功なら true。false なら Gradle はタスクを失敗にする
     */
    @JvmStatic
    public fun run(
        modelJson: String,
        unitName: String,
        forceFull: Boolean,
        log: BiConsumer<String, String>,
    ): Boolean {
        val model = json.decodeFromString(ProjectModel.serializer(), modelJson)
        val result = AnalysisCodegenEngine().run(EngineRequest(model = model, unitName = unitName, forceFull = forceFull))
        result.diagnostics.forEach { d ->
            val severity =
                when (d.severity) {
                    Diagnostic.Severity.Error -> "error"
                    Diagnostic.Severity.Warning -> "warning"
                    Diagnostic.Severity.Info -> "info"
                }
            log.accept(severity, format(d))
        }
        return result.success
    }

    private fun format(d: Diagnostic): String =
        buildString {
            d.processorName?.let { append("[").append(it).append("] ") }
            if (d.file != null) {
                append(d.file)
                d.line?.let { append(":").append(it) }
                append(": ")
            }
            append(d.message)
        }
}
