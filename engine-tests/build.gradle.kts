// エンジン内部（analysis-engine・processing-engine・incremental-engine の @InternalAnalysisCodegenApi）や
// AA の型に触れる I / U のケースを置く（ADR-034）。e2e-tests は AA なしでコンパイルできるままにする。
// パッケージは観点ごとに分ける（me.tbsten.analysiscodegen.enginetests.<観点>）。support パッケージは ACG-44 が持つ
plugins {
    id("acg.tests")
    // AA の型（KtFile・KaType など）をテストから直接使う（ADR-038）
    id("acg.analysis-api")
}

dependencies {
    testImplementation(project(":testing"))
    // KMP-23: Gradle plugin のタスク名・出力先の命名（ADR-016）
    testImplementation(project(":gradle-plugin"))
    testImplementation(project(":analysis-engine")) // ACG-46: MEM-01（解析環境、ADR-037）
    testImplementation(project(":project-model")) // ACG-46: MEM-01 のモデル
    testImplementation(project(":pbt")) // ACG-46: XD-05・XD-10 のカタログと変更の列（ADR-022）
}

kotlin {
    compilerOptions {
        // AA の実験的 API（expect / actual の対応など）をテストから使う
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
    }
}
