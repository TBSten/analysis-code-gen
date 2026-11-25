plugins {
    id("acg.tests")
}

dependencies {
    testImplementation(project(":testing"))
    testImplementation(gradleTestKit())
    testImplementation(project(":test-processors:ksp"))
    testImplementation(libs.kotlinpoet.ksp)
    // GCC-04（U）: ProjectBuilder で plugin を適用してタスクの入力を調べる。gradle-plugin は AA に依存しない（ADR-012）
    testImplementation(project(":gradle-plugin"))
    testImplementation(gradleApi())
    testImplementation(libs.kotlin.gradle.plugin)
}

tasks.withType<Test>().configureEach {
    // F-* フィクスチャを複製して動かす補助（e2e.support、ADR-032）がリポジトリのルートを知るため
    systemProperty("acg.repoRoot", rootDir.absolutePath)
    // TestKit のフィクスチャが使う Kotlin（ADR-011）と Maven ミラー（クラウド環境向け、ADR-025）
    providers.gradleProperty("acg.kotlin").orNull?.let { systemProperty("acg.kotlin", it) }
    providers.gradleProperty("acg.mavenMirror").orNull?.let { systemProperty("acg.mavenMirror", it) }
}
