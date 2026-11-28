package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * plugin id `me.tbsten.analysis.code.gen`（ADR-016）。
 *
 * kotlin("jvm") / kotlin("multiplatform") / kotlin("android")（AGP の組み込み Kotlin を含む）のどれも無ければ、
 * プロジェクトの評価が終わった時点で失敗する（GRD-28）。plugin の適用順に依存しないよう、Kotlin の検出は遅らせる。
 *
 * KGP が classpath に無いビルドでも原因の分かるエラーを出せるよう、このクラスは KGP の型を参照しない。
 * KGP の型に触れるのは [AnalysisCodegenKotlinWiring] だけで、KGP があると分かってから作る。
 */
public abstract class AnalysisCodegenPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create(EXTENSION_NAME, AnalysisCodegenExtension::class.java)
        val kgpAvailable =
            runCatching { Class.forName(KOTLIN_EXTENSION_CLASS, false, AnalysisCodegenPlugin::class.java.classLoader) }.isSuccess
        var wiring: AnalysisCodegenKotlinWiring? = null
        fun wireIfKotlin() {
            if (wiring != null || !kgpAvailable) return
            wiring = AnalysisCodegenKotlinWiring.wireIfKotlin(target, extension)
        }
        KOTLIN_PLUGIN_IDS.forEach { id -> target.pluginManager.withPlugin(id) { wireIfKotlin() } }
        target.afterEvaluate {
            wireIfKotlin()
            val wired =
                wiring ?: throw GradleException(
                    "analysisCodeGen: プロジェクト '${target.path}' に Kotlin の plugin が無い。" +
                        "kotlin(\"jvm\") / kotlin(\"multiplatform\") / kotlin(\"android\") のどれかを plugins { } に足す" +
                        "（Android は AGP の組み込み Kotlin でもよい）",
                )
            wired.onEvaluated()
        }
    }

    public companion object {
        public const val ID: String = "me.tbsten.analysis.code.gen"
        public const val EXTENSION_NAME: String = "analysisCodeGen"

        private const val KOTLIN_EXTENSION_CLASS = "org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension"

        private val KOTLIN_PLUGIN_IDS =
            listOf(
                "org.jetbrains.kotlin.jvm",
                "org.jetbrains.kotlin.multiplatform",
                "org.jetbrains.kotlin.android",
                "com.android.base",
            )
    }
}
