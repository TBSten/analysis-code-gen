// エンジン内部（analysis-code-gen-analysis-engine・analysis-code-gen-processing-engine・
// analysis-code-gen-incremental-engine の @InternalAnalysisCodegenApi）や
// AA の型に触れる I / U のケースを置く（ADR-034）。e2e-tests は AA なしでコンパイルできるままにする。
// パッケージは観点ごとに分ける（me.tbsten.analysis.code.gen.enginetests.<観点>）。support パッケージは ACG-44 が持つ
// AA のリポジトリにクラウドの Agent 環境から届かないため、このモジュールの判定は CI に任せる
plugins {
    id("acg.tests")
    // AA の型（KtFile・KaType など）をテストから直接使う（ADR-038）
    id("acg.analysis-api")
}

dependencies {
    testImplementation(project(":analysis-code-gen-testing"))
    testImplementation(project(":pbt"))
    testImplementation(project(":analysis-code-gen-project-model"))
    testImplementation(project(":analysis-code-gen-processor-api"))
    testImplementation(project(":analysis-code-gen-analysis-engine"))
    testImplementation(project(":analysis-code-gen-processing-engine"))
    testImplementation(project(":analysis-code-gen-incremental-engine"))
    testImplementation(project(":analysis-code-gen-ksp-bridge-api"))
    testImplementation(project(":analysis-code-gen-ksp-bridge"))
    testImplementation(libs.ksp.api)
    // KMP-23: Gradle plugin のタスク名・出力先の命名（ADR-016）
    testImplementation(project(":analysis-code-gen-gradle-plugin"))
    // AA Standalone を実際に動かす一式（ADR-060）。Worker に載せる一覧と同じ bundle を使う
    testRuntimeOnly(libs.bundles.aa.runtime) { isTransitive = false }
}

kotlin {
    compilerOptions {
        // AA の実験的 API（expect / actual の対応など）をテストから使う
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
    }
}

// AA の *-for-ide と kct の kotlin-compiler-embeddable は org.jetbrains.kotlin.cli.* を同じ名前で持つ。
// 解析環境（テストから AA の型を直接使う）が for-ide 側を読むよう、AA の一式を実行時クラスパスの先頭に置く（ADR-060）
val aaRuntimeFirst: Configuration by configurations.creating {
    isTransitive = false
}
dependencies { aaRuntimeFirst(libs.bundles.aa.runtime) }
tasks.withType<Test>().configureEach {
    classpath = aaRuntimeFirst + classpath
}
