// KSP 互換テスト（ADR-024・064）。KSP 公式テスト（KSPT）を、公式 KSP2（K、ksptKsp2Test タスク）と Bridge（B、test タスク）で流す
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

    // K 側はコンパイラ（kct fork が持つ kotlin-compiler-embeddable）と KSP2 だけで動く
    testImplementation(project(":analysis-code-gen-testing-kct"))
    testImplementation(kspt.output)
    testImplementation(libs.ksp.api)
    testImplementation(libs.ksp.aa.embeddable)
    testImplementation(libs.ksp.common.deps)
    testImplementation(kotlin("reflect"))

    // B 側はエンジン（ProjectModel を組んで AnalysisCodegenEngine で回す）と、ServiceLoader で見つかる Bridge で動く（ADR-064）
    testImplementation(project(":analysis-code-gen-processing-engine"))
    testRuntimeOnly(project(":analysis-code-gen-ksp-bridge"))
}

// B を動かす AA Standalone の一式（ADR-060）。test タスクだけ、実行時クラスパスの先頭に置く（ADR-064）:
// - AA の *-for-ide と kotlin-compiler-embeddable は org.jetbrains.kotlin.cli.* を同じ名前で持つ。エンジンが for-ide 側を読むよう先頭にし、
//   kotlinc は KsptCompilers が AA を除いた ClassLoader で動かす
// - 公式 KSP2（K）は AA を ksp. 接頭辞へ移した jar で動くが、IntelliJ の拡張定義（XML）を名前で読むため、AA の一式と同じ ClassLoader では
//   壊れる（KotlinAsJavaSupport が見つからない）。K は AA の一式を載せない ksptKsp2Test タスクで流す
val aaRuntimeFirst: Configuration by configurations.creating {
    isTransitive = false
}
dependencies { aaRuntimeFirst(libs.bundles.aa.runtime) }

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

// 写したコードは上流との差分を小さく保つため整形しない
tasks.matching { it.name.startsWith("ktlint") && it.name.contains("Kspt") }.configureEach { enabled = false }

val ksptKsp2Test =
    tasks.register<Test>("ksptKsp2Test") {
        description = "取り込んだ KSPT が公式 KSP2（K）で通ることを確かめる（KsptKsp2Test、ADR-024・064）"
        group = "verification"
        testClassesDirs =
            sourceSets.test
                .get()
                .output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        filter.includeTestsMatching("*.KsptKsp2Test")
    }

tasks.test {
    classpath = aaRuntimeFirst + classpath
    filter.excludeTestsMatching("*.KsptKsp2Test")
    // acg.tests はテストの前に build/acg の保留の結果を消す。K のタスク（保留のケースが無い）が後から消さないよう、K を先に流す
    mustRunAfter(ksptKsp2Test)
}

tasks.check { dependsOn(ksptKsp2Test) }

tasks.withType<Test>().configureEach {
    providers.gradleProperty("acg.kspt.k").orNull?.let { systemProperty("acg.kspt.k", it) }
    providers.gradleProperty("acg.kspt.only").orNull?.let { systemProperty("acg.kspt.only", it) }
    providers.gradleProperty("acg.kspt.jdkHome").orNull?.let { systemProperty("acg.kspt.jdkHome", it) }
    // KSP2（AA）を同じ JVM で何度も立ち上げるため
    maxHeapSize = "3g"
}
