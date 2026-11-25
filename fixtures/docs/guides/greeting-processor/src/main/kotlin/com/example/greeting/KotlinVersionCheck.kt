package com.example.greeting

import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment

/** 実行中の Analysis API（= 利用者の Kotlin）の版で、使う機能を切り替える例。 */
fun supportsNewSyntax(environment: AnalysisCodegenProcessorEnvironment): Boolean {
    val (major, minor) = environment.kotlinVersion.split(".").map { it.takeWhile(Char::isDigit).toInt() }
    val supported = major > 2 || (major == 2 && minor >= 5)
    if (!supported) {
        environment.logger.info("greeting: Kotlin ${environment.kotlinVersion} では新しい文法を扱いません")
    }
    return supported
}
