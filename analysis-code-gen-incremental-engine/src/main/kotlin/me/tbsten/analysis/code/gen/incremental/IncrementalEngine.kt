@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.incremental

import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import java.io.File

/**
 * 増分処理 Level 2（ADR-015）。AA に依存しない。ファイルはすべて rootDir からの相対パス。
 */
@InternalAnalysisCodegenApi
public data class InputSnapshot(
    /** ソースの相対パス → 内容のハッシュ。 */
    val sources: Map<String, String>,
    /** これが変わったら全件処理: クラスパス、コンパイラオプション、Processor の JAR とオプション、各バージョン。 */
    val globalFingerprint: String,
    val externalInputs: Map<String, String>,
)

@InternalAnalysisCodegenApi
public enum class OutputDependencyKind { Isolating, Aggregating, All }

@InternalAnalysisCodegenApi
public data class OutputRecord(
    val outputPath: String,
    val processorName: String,
    val kind: OutputDependencyKind,
    val sources: Set<String>,
    val externalInputs: Set<String>,
)

@InternalAnalysisCodegenApi
public sealed interface IncrementalPlan {
    /** 全件処理。[reason] は診断に出す。 */
    public data class Full(val reason: String) : IncrementalPlan

    public data class Incremental(
        val dirtySources: Set<String>,
        val changedSources: Set<String>,
        val removedSources: Set<String>,
        /** 今回 Processor が作り直さなければ消す出力。 */
        val staleOutputs: Set<String>,
    ) : IncrementalPlan
}

@InternalAnalysisCodegenApi
public interface IncrementalState {
    public val snapshot: InputSnapshot
    public val outputs: List<OutputRecord>
}

@InternalAnalysisCodegenApi
public class DirtyFileCalculator {
    /** 前回の状態と今回の入力から、処理の計画を立てる。[extraDirty] は Bridge の Lookup 追跡など。 */
    public fun plan(
        previous: IncrementalState?,
        current: InputSnapshot,
        extraDirty: (changed: Set<String>, removed: Set<String>) -> Set<String>?,
    ): IncrementalPlan = TODO()
}

/** `build/analysisCodeGen/caches/<unit>/` の読み書き。形式の番号とバージョンが違えば読まない。 */
@InternalAnalysisCodegenApi
public class PersistentCache(public val dir: File) {
    public fun load(expectedFormat: String): IncrementalState? = TODO()

    public fun save(
        format: String,
        state: IncrementalState,
    ): Unit = TODO()
}
