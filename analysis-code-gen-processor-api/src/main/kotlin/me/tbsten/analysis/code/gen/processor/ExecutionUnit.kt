package me.tbsten.analysis.code.gen.processor

import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

/**
 * Processor を実行する単位（ADR-013・016）。
 */
public sealed interface ExecutionUnit {
    /** タスク名・出力ディレクトリに使う名前。例: `commonMain`、`jvmMain`（Compilation）。 */
    public val name: String

    /** SourceSet ごとの実行。 */
    public interface SourceSetUnit : ExecutionUnit

    /** Compilation ごとの実行。 */
    public interface CompilationUnit : ExecutionUnit
}

/** どのファイルを Processor の処理対象にするか（設計文書 5.3）。 */
public enum class ProcessingScope {
    /** 自分の SourceSet のファイルだけ（既定）。 */
    Own,

    /** `dependsOn` を遡った上流の SourceSet のファイルも含める。出力先は変わらない。 */
    IncludeDependsOn,
}

public interface SourceSetContext {
    /** KGP 上の SourceSet 名。例: `commonMain`、`main`。 */
    public val name: String

    public val platform: TargetPlatform

    /** この SourceSet の Kotlin ソース + この実行単位が前のラウンドまでに生成した Kotlin ファイル。 */
    public val ownFiles: List<KtFile>

    /** ownFiles + `dependsOn` を遡った上流のソースと生成物。ライブラリは含まない。 */
    public val visibleFiles: List<KtFile>

    /** `dependsOn` の直接の上流。 */
    public val dependencies: List<SourceSetContext>

    public val module: KaSourceModule
}

public interface CompilationContext {
    /** KGP の target 名。kotlin("jvm") では空文字。 */
    public val targetName: String

    /** KGP の Compilation 名。例: `main`、`test`、Android では Variant 名。 */
    public val compilationName: String

    public val platform: TargetPlatform

    /** この Compilation に参加するすべての SourceSet。 */
    public val sourceSets: List<SourceSetContext>

    /** 生成物の所有者となる default SourceSet。 */
    public val defaultSourceSet: SourceSetContext
}
