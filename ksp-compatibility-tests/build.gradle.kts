// KSP 互換テスト（ADR-024）。KSP 公式テスト（KSPT）を、公式 KSP2（K）と Bridge（B）で流す
// - src/kspt: 上流から写したテスト用 Processor（porting/ksp/import_kspt.py が書く。手で直さない）
// - src/test/resources/kspt: 上流から写した testData と、取り込んだテストの一覧（cases.csv）
// - K で実際に通るかの確認は時間がかかるため、-Pacg.kspt.k=true のときだけ全件を流す（既定は数件だけ）
plugins {
    id("acg.tests")
}

val kspt: SourceSet by sourceSets.creating

dependencies {
    // KSPT の Processor は KSP の API と、一部は KSP2 の実装クラス（ResolverAAImpl など）に依存する
    "ksptImplementation"(libs.ksp.api)
    "ksptImplementation"(libs.ksp.aa.embeddable)
    "ksptImplementation"(kotlin("reflect"))

    testImplementation(project(":ksp-bridge"))
    testImplementation(project(":testing"))
    testImplementation(kspt.output)
    testImplementation(libs.ksp.api)
    testImplementation(libs.ksp.aa.embeddable)
    testImplementation(libs.ksp.common.deps)
    testImplementation(kotlin("reflect"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

// 写したコードは上流との差分を小さく保つため整形しない
tasks.matching { it.name.startsWith("ktlint") && it.name.contains("Kspt") }.configureEach { enabled = false }

tasks.withType<Test>().configureEach {
    providers.gradleProperty("acg.kspt.k").orNull?.let { systemProperty("acg.kspt.k", it) }
    providers.gradleProperty("acg.kspt.only").orNull?.let { systemProperty("acg.kspt.only", it) }
    providers.gradleProperty("acg.kspt.jdkHome").orNull?.let { systemProperty("acg.kspt.jdkHome", it) }
    // KSP2（AA）を同じ JVM で何度も立ち上げるため
    maxHeapSize = "3g"
}
