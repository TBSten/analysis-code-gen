package me.tbsten.analysis.code.gen.processor

import com.intellij.psi.PsiFile
import java.io.File

/**
 * 生成物がどの入力に依存するか（ADR-015）。迷ったら [all] を使う（遅いが正しい）。
 */
public sealed class Dependencies {
    /** Processor が読んだ、ソース以外のファイル（リソースなど）。変わると再生成する。 */
    public abstract val externalInputs: List<File>

    /** 出力は [files] だけに依存する。 */
    public class Isolating internal constructor(
        public val files: List<PsiFile>,
        override val externalInputs: List<File>,
    ) : Dependencies()

    /** [files] に加え、Processing Scope のファイル集合（増減）に依存する。Registry などの集約出力向け。 */
    public class Aggregating internal constructor(
        public val files: List<PsiFile>,
        override val externalInputs: List<File>,
    ) : Dependencies()

    /** Processing Scope の全ファイルに依存する。依存を宣言しなかった出力もこれとして扱う。 */
    public class All internal constructor(
        override val externalInputs: List<File>,
    ) : Dependencies()

    /** [inputs] を external inputs に足したコピーを返す。 */
    public fun withExternalInputs(vararg inputs: File): Dependencies {
        val merged = (externalInputs + inputs).distinct()
        return when (this) {
            is Isolating -> Isolating(files, merged)
            is Aggregating -> Aggregating(files, merged)
            is All -> All(merged)
        }
    }

    public companion object {
        public fun isolating(vararg files: PsiFile): Dependencies = isolating(files.asList())

        public fun isolating(files: Iterable<PsiFile>): Dependencies = Isolating(files.distinct(), emptyList())

        public fun aggregating(vararg files: PsiFile): Dependencies = aggregating(files.asList())

        public fun aggregating(files: Iterable<PsiFile>): Dependencies = Aggregating(files.distinct(), emptyList())

        public val all: Dependencies get() = All(emptyList())
    }
}
