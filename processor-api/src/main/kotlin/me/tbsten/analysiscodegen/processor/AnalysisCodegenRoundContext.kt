package me.tbsten.analysiscodegen.processor

import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtFile

/**
 * 1 ラウンド分の入力と生成 API。ラウンドが終わったら使わない。
 *
 * ファイル集合の定義は ADR-013 の表のとおり。どのリストもパスの辞書順。
 */
public interface AnalysisCodegenRoundContext {
    /** 1 から始まるラウンド番号。 */
    public val round: Int

    public val executionUnit: ExecutionUnit

    /**
     * 生成物の所有者となる SourceSet。Compilation 単位の実行では、その Compilation の default SourceSet。
     */
    public val sourceSet: SourceSetContext

    /** Compilation 単位の実行のときだけ非 null。 */
    public val compilation: CompilationContext?

    /** Processing Scope の Kotlin ファイル（既定は own files）。 */
    public val allFiles: List<KtFile>

    /** Processing Scope の Java ソース。 */
    public val allJavaFiles: List<PsiJavaFile>

    /**
     * ラウンド 1: 今回処理すべきファイル（全件ビルドなら [allFiles]、増分なら dirty なファイル）。
     * ラウンド 2 以降: 直前のラウンドでこの実行単位が生成した Kotlin ファイル。
     */
    public val newFiles: List<KtFile>

    /** ラウンド 1 の [newFiles] のうち、前回のビルドにも存在し内容が変わったもの。それ以外は空。 */
    public val changedFiles: List<KtFile>

    /** ラウンド 1 だけ、前回あって今回消えたソースの、SourceSet ルートからの相対パス。それ以外は空。 */
    public val removedFilePaths: Set<String>

    public val codeGenerator: CodeGenerator

    public val logger: CodegenLogger

    /** [AnalysisCodegenProcessorEnvironment.options] と同じ。 */
    public val options: Map<String, String>

    /** 前のラウンドでこの Processor が defer し、今のラウンドで復元できたもの（ADR-014）。 */
    public val deferred: List<DeferredSymbol>

    /**
     * まだ処理できないシンボルを次のラウンドへ持ち越す。
     *
     * @param payload Processor が自由に使える文字列（どの処理の途中だったか等）。
     */
    public fun defer(
        pointer: KaSymbolPointer<KaSymbol>,
        payload: String? = null,
    )
}

/**
 * ラウンドをまたいで持ち越したシンボル。[pointer] は有効な `analyze { }` の中で `restoreSymbol()` して使う。
 */
public class DeferredSymbol(
    public val pointer: KaSymbolPointer<KaSymbol>,
    public val payload: String?,
)
