// 三者差分ハーネス（ACG-42、ADR-023）。同じ入力を K（KSP2）/ B（Bridge）/ N（Native 移植版）で動かし、
// 出力を集めて正規化して比べる。K の結果は「入力のハッシュ → 出力」でキャッシュする。
// 利用者には公開しない内部モジュール。XD-* のテストと PBT・ベンチマークから使う
plugins {
    id("acg.jvm")
    id("acg.tests")
}

val libs = versionCatalogs.named("libs")

dependencies {
    api(project(":analysis-code-gen-testing"))
    testImplementation(project(":test-processors:ksp"))
    // XD-16 の性質テストは PBT 基盤の acgCheckAll で回す（ADR-022）
    testImplementation(project(":pbt"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.withType<Test>().configureEach {
    // K 結果のキャッシュの鍵に入れる KSP の版（ADR-023）。jar 名からも読めるが、catalog の値を正とする
    systemProperty("acg.ksp.version", libs.findVersion("ksp").get().requiredVersion)
    // テストごとのキャッシュは build 配下に置く（CI では actions/cache で .acg-k-cache を持ち回る想定）
    systemProperty(
        "acg.diff.cacheDir",
        layout.buildDirectory
            .dir("acg/k-cache")
            .get()
            .asFile.absolutePath,
    )
}
