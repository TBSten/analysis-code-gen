// 利用者向けテスト補助（ADR-021）。Processor のテスト API（runCodegenTest / runCodegenScenario）と
// Gradle TestKit のフィクスチャ補助（gradle パッケージ）。コンパイルと KSP2 の実行は analysis-code-gen-testing-kct（kct fork）に任せる
plugins {
    id("acg.library")
    id("acg.dokka")
}

dependencies {
    api(project(":analysis-code-gen-processor-api"))
    api(project(":analysis-code-gen-testing-kct"))
    implementation(project(":analysis-code-gen-processing-engine"))
    // ksp(provider) を Bridge で回す（ADR-017・050）。エンジンは ServiceLoader で Frontend を見つける
    runtimeOnly(project(":analysis-code-gen-ksp-bridge"))
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
