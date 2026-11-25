package com.example.registry

import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.ExecutionUnit

/** 一覧のコメントに書く、どの実行単位で作ったかの説明。 */
fun describeUnit(context: AnalysisCodegenRoundContext): String =
    when (context.executionUnit) {
        // Compilation ごと: target と Compilation の名前が取れる
        is ExecutionUnit.CompilationUnit -> {
            val compilation = context.compilation!!
            "${compilation.targetName} の ${compilation.compilationName}"
        }
        // SourceSet ごと: compilation は null
        is ExecutionUnit.SourceSetUnit -> context.sourceSet.name
    }
