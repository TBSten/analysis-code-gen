package me.tbsten.analysiscodegen.gradle

import org.gradle.api.Named
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * SourceSet / Compilation ごとの設定の共通部分（ADR-016）。
 *
 * - [processor] はその実行単位の Configuration（`analysisCodeGen<Name>` など）に依存を足す
 * - 同じキーに違う値の [arg] が 2 回書かれたら設定エラー
 */
public interface AnalysisCodegenUnitSpec : Named {
    /** `"group:name:version"`、`project(":p")`、`libs.xxx` など `dependencies {}` に書けるもの。 */
    public fun processor(dependencyNotation: Any)

    public val args: MapProperty<String, String>

    public fun arg(key: String, value: String)
}

public abstract class AnalysisCodegenSourceSetSpec : AnalysisCodegenUnitSpec {
    /** 既定 [ProcessingScope.Own]。 */
    public abstract val processingScope: Property<ProcessingScope>

    override fun processor(dependencyNotation: Any): Unit = TODO()

    override fun arg(key: String, value: String): Unit = TODO()
}

public abstract class AnalysisCodegenCompilationSpec : AnalysisCodegenUnitSpec {
    override fun processor(dependencyNotation: Any): Unit = TODO()

    override fun arg(key: String, value: String): Unit = TODO()
}

/** processor-api の ProcessingScope と同じ意味（DSL 側の型。gradle-plugin は processor-api に依存しない）。 */
public enum class ProcessingScope { Own, IncludeDependsOn }
