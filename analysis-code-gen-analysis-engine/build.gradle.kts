plugins {
    id("acg.library")
    id("acg.analysis-api")
}

dependencies {
    api(project(":analysis-code-gen-project-model"))
    // InternalAnalysisCodegenApi の印だけを使う
    implementation(project(":analysis-code-gen-processor-api"))
    // AA Standalone の環境構築に要る内部 API（KSP2 の kotlin-analysis-api と同じ範囲。ADR-060）。実行時は利用者の版を Worker が載せる（ADR-011）
    compileOnly(libs.bundles.aa.standalone.compile) { isTransitive = false }
    compileOnly(libs.aa.dep.coroutines)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
        )
    }
}
