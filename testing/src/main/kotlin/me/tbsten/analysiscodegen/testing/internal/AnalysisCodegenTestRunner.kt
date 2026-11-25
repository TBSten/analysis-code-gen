package me.tbsten.analysiscodegen.testing.internal

import me.tbsten.analysiscodegen.testing.CodegenTestResult
import me.tbsten.analysiscodegen.testing.CodegenTestSpec

/**
 * [me.tbsten.analysiscodegen.testing.TestEngine.AnalysisCodegen]: spec を ProjectModel にして AnalysisCodegenEngine で回し、
 * 生成物を kct fork でコンパイルする（ADR-021）。エンジン（P2）ができるまでは未実装。
 */
internal object AnalysisCodegenTestRunner {
    fun run(spec: CodegenTestSpec): CodegenTestResult =
        throw NotImplementedError("TestEngine.AnalysisCodegen はエンジンの実装（P2）後に動く。spec=${spec.sourceSets.keys}")
}
