// エンジン内部（analysis-engine・processing-engine・incremental-engine の @InternalAnalysisCodegenApi）や
// AA の型に触れる I / U のケースを置く（ADR-034）。e2e-tests は AA なしでコンパイルできるままにする。
// パッケージは観点ごとに分ける（me.tbsten.analysiscodegen.enginetests.<観点>）。support パッケージは ACG-44 が持つ
plugins {
    id("acg.tests")
}

dependencies {
    testImplementation(project(":testing"))
}
