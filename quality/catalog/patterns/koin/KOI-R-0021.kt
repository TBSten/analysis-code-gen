// PATTERN: KOI-R-0021
// TITLE: codymikol/git-down / root / CompositeGrammarDownloader
// PROCESSORS: koin
// EXPECT: ok
// ORIGIN: real:codymikol/git-down@dfbb1e11eab9/src/main/kotlin/com/codymikol/highlighting/CompositeGrammarDownloader.kt
// LICENSE: MIT
// STATUS: rejected
// NOTE: ファイル単体の抜き出し。同じプロジェクトの他の型やライブラリに依存するので K（KSP2 2.3.14）: K でビルドが失敗した: Unresolved reference 'PrepackagedGrammarDownloader'.
// FILE: root/main/com/codymikol/highlighting/CompositeGrammarDownloader.kt
package com.codymikol.highlighting

import org.koin.core.annotation.Single
import java.nio.file.Path

/**
 * Prefers [PrepackagedGrammarDownloader]'s classpath-resource extraction, falling back to
 * [SourceCompilingGrammarDownloader]'s GitHub-fetch-and-compile path only for the few grammars
 * upstream doesn't publish a prepackaged io.github.bonede artifact for (vim, csv, tcl).
 */
@Single
class CompositeGrammarDownloader(
    private val prepackaged: PrepackagedGrammarDownloader,
    private val sourceCompiling: SourceCompilingGrammarDownloader,
) : GrammarDownloader {

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean =
        prepackaged.download(spec, destination) || sourceCompiling.download(spec, destination)

}
