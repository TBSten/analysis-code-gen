package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * plugin id `me.tbsten.analysis.code.gen`（ADR-016）。
 *
 * kotlin("jvm") / kotlin("multiplatform") / kotlin("android") のどれも無ければ apply の時点で失敗する（GRD-28）。
 */
public abstract class AnalysisCodegenPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = TODO()

    public companion object {
        public const val ID: String = "me.tbsten.analysis.code.gen"
        public const val EXTENSION_NAME: String = "analysisCodeGen"
    }
}
