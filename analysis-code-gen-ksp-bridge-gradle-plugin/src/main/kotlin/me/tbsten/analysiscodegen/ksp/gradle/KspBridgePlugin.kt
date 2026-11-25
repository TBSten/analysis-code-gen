package me.tbsten.analysiscodegen.ksp.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * plugin id `me.tbsten.analysis.code.gen.ksp-bridge`（ADR-016・017）。本体の plugin も適用し、
 * `analysisCodeGen { kspBridge { } }` と Bridge のランタイムを足す。
 */
public abstract class KspBridgePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = TODO()

    public companion object {
        public const val ID: String = "me.tbsten.analysis.code.gen.ksp-bridge"
        public const val EXTENSION_NAME: String = "kspBridge"
    }
}

/** `analysisCodeGen { kspBridge { } }`。 */
public abstract class KspBridgeExtension {
    /** 既定 [KspProcessingMode.Compatible]。 */
    public abstract val processingMode: Property<KspProcessingMode>

    /** Processor がビルドされた KSP API のバージョンを検査する。既定 true。 */
    public abstract val kspVersionCheck: Property<Boolean>
}

public enum class KspProcessingMode {
    /** KSP2 と同じく、KSP Processor に Compilation の全ソースを見せる（既定）。 */
    Compatible,

    /** 実行単位の Processing Scope だけを見せる。共通の生成が 1 回で済む。 */
    SourceSet,
}
