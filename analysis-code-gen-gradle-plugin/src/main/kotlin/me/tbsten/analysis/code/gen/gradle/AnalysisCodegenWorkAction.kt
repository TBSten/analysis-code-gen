package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.lang.reflect.InvocationTargetException
import java.util.function.BiConsumer

/**
 * Worker の中でエンジンを動かす（ADR-016、ADR-063）。
 *
 * このクラスは Worker の隔離 ClassLoader（エンジンと AA を含む）で読み込まれる。gradle-plugin はエンジンを import しない
 * （ADR-012、GRD-21）ので、入口 `EngineEntryPoint.run(modelJson, unitName, forceFull, log)` をリフレクションで呼ぶ。
 * 受け渡しは JDK の型だけにし、ClassLoader をまたいでも型がずれないようにする。
 */
public abstract class AnalysisCodegenWorkAction : WorkAction<AnalysisCodegenWorkAction.Parameters> {
    public interface Parameters : WorkParameters {
        public val modelFile: RegularFileProperty
        public val unitName: Property<String>
        public val forceFull: Property<Boolean>
        public val isolation: Property<String>
    }

    override fun execute() {
        val unit = parameters.unitName.get()
        val logger = Logging.getLogger(AnalysisCodegenWorkAction::class.java)
        val loader = AnalysisCodegenWorkAction::class.java.classLoader
        // ADR-039: Worker と ClassLoader の再利用を --info で追えるようにする
        logger.info(
            "analysisCodeGen[$unit]: engineClassLoader=${System.identityHashCode(loader)} worker=${
                parameters.isolation.get().replaceFirstChar { it.lowercaseChar() }
            }",
        )
        val entry =
            try {
                Class.forName(ENTRY_POINT, true, loader)
            } catch (e: ClassNotFoundException) {
                throw GradleException("analysisCodeGen: エンジンの入口 $ENTRY_POINT が Worker のクラスパスに無い", e)
            }
        val run =
            entry.getMethod(
                "run",
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                BiConsumer::class.java,
            )
        val log =
            BiConsumer<String, String> { severity, line ->
                when (severity) {
                    "error" -> logger.error("e: $line")
                    "warning" -> logger.warn("w: $line")
                    "lifecycle" -> logger.lifecycle(line)
                    "debug" -> logger.debug(line)
                    else -> logger.info("i: $line")
                }
            }
        val modelJson = parameters.modelFile.get().asFile.readText()
        val success =
            try {
                run.invoke(null, modelJson, unit, parameters.forceFull.get(), log) as Boolean
            } catch (e: InvocationTargetException) {
                throw GradleException("analysisCodeGen[$unit]: エンジンが例外で止まった: ${e.targetException}", e.targetException)
            }
        if (!success) {
            throw GradleException("analysisCodeGen[$unit]: 処理に失敗した（上の e: の診断を見る）")
        }
    }

    private companion object {
        const val ENTRY_POINT = "me.tbsten.analysis.code.gen.engine.EngineEntryPoint"
    }
}
