// エンジン内部（analysis-engine・processing-engine・incremental-engine の @InternalAnalysisCodegenApi）や
// AA の型に触れる I / U のケースを置く（ADR-034）。e2e-tests は AA なしでコンパイルできるままにする。
// パッケージは観点ごとに分ける（me.tbsten.analysiscodegen.enginetests.<観点>）。support パッケージは ACG-44 が持つ
// AA のリポジトリにクラウドの Agent 環境から届かないため、このモジュールの判定は CI に任せる
plugins {
    id("acg.tests")
    // AA の型（KtFile・KaType など）をテストから直接使う（ADR-038）
    id("acg.analysis-api")
}

dependencies {
    testImplementation(project(":testing"))
    testImplementation(project(":pbt"))
    testImplementation(project(":project-model"))
    testImplementation(project(":processor-api"))
    testImplementation(project(":analysis-engine"))
    testImplementation(project(":processing-engine"))
    testImplementation(project(":incremental-engine"))
    testImplementation(project(":ksp-bridge-api"))
    testImplementation(project(":ksp-bridge"))
    testImplementation(libs.ksp.api)
    // KMP-23: Gradle plugin のタスク名・出力先の命名（ADR-016）
    testImplementation(project(":gradle-plugin"))
}

kotlin {
    compilerOptions {
        // AA の実験的 API（expect / actual の対応など）をテストから使う
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
    }
}
