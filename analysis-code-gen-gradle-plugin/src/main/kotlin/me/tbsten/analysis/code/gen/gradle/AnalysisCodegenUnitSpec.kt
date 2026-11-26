package me.tbsten.analysis.code.gen.gradle

import org.gradle.api.Named
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

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

    public fun arg(
        key: String,
        value: String,
    )
}

public abstract class AnalysisCodegenSourceSetSpec : AnalysisCodegenUnitSpec {
    /** 処理対象の広さ。既定は自分の SourceSet だけで、`processingScope.includeDependsOn()` で上流も含める。 */
    @get:Nested
    public abstract val processingScope: ProcessingScopeSpec

    override fun processor(dependencyNotation: Any): Unit = TODO()

    override fun arg(
        key: String,
        value: String,
    ): Unit = TODO()
}

public abstract class AnalysisCodegenCompilationSpec : AnalysisCodegenUnitSpec {
    override fun processor(dependencyNotation: Any): Unit = TODO()

    override fun arg(
        key: String,
        value: String,
    ): Unit = TODO()
}

/**
 * SourceSet の処理対象の広さを選ぶ DSL（`processingScope.includeDependsOn()`）。
 * 値は [value] に入り、既定は [ProcessingScope.Own]。
 */
public abstract class ProcessingScopeSpec {
    public abstract val value: Property<ProcessingScope>

    /** 自分の SourceSet のファイルだけを処理対象にする（既定）。 */
    public fun own() {
        value.set(ProcessingScope.Own)
    }

    /** dependsOn でたどれる上流の SourceSet のファイルも処理対象にする。出力先は変わらない。 */
    public fun includeDependsOn() {
        value.set(ProcessingScope.IncludeDependsOn)
    }
}

/**
 * analysis-code-gen-processor-api の ProcessingScope と同じ意味。
 * DSL 側の型（analysis-code-gen-gradle-plugin は analysis-code-gen-processor-api に依存しない）。
 */
public enum class ProcessingScope { Own, IncludeDependsOn }
