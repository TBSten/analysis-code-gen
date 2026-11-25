plugins {
    id("acg.tests")
}

dependencies {
    testImplementation(project(":testing"))
    testImplementation(gradleTestKit())
    testImplementation(project(":test-processors:ksp")) // ACG-46: TST の Processor（ADR-027）
    testImplementation(project(":pbt")) // ACG-46: PBT-03〜05（ADR-022）
    testImplementation(project(":differential")) // ACG-46: XD（ADR-023）
}

tasks.withType<Test>().configureEach {
    // F-* フィクスチャを複製して動かす補助（e2e.support、ADR-032）がリポジトリのルートを知るため
    systemProperty("acg.repoRoot", rootDir.absolutePath)
    // TestKit のフィクスチャが使う Kotlin（ADR-011）と Maven ミラー（クラウド環境向け、ADR-025）
    providers.gradleProperty("acg.kotlin").orNull?.let { systemProperty("acg.kotlin", it) }
    providers.gradleProperty("acg.mavenMirror").orNull?.let { systemProperty("acg.mavenMirror", it) }
}
