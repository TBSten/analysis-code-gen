// 利用者向けテスト補助（ADR-021）。Processor のテスト API（runCodegenTest / runCodegenScenario）と
// Gradle TestKit のフィクスチャ補助（gradle パッケージ）。コンパイルと KSP2 の実行は testing-kct（kct fork）に任せる
plugins {
    id("acg.library")
    id("acg.dokka")
}

dependencies {
    api(project(":processor-api"))
    api(project(":testing-kct"))
    implementation(project(":processing-engine"))
    // TestKit は利用者のビルドが持っているものを使う（Maven に公開されていないため compileOnly）
    compileOnly(gradleTestKit())

    testImplementation(gradleTestKit())
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.withType<Test>().configureEach {
    // TestKit のフィクスチャが使う Kotlin（ADR-011）と Maven ミラー（クラウド環境向け、ADR-025）
    providers.gradleProperty("acg.kotlin").orNull?.let { systemProperty("acg.kotlin", it) }
    providers.gradleProperty("acg.mavenMirror").orNull?.let { systemProperty("acg.mavenMirror", it) }
}
