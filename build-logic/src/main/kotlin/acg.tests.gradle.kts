// ケース ID 付きテスト（ACG-37、ADR-030）を持つモジュールに適用する。
// - テストから :test-foundation（AcgSpec・PendingCase）を使えるようにする
// - collectCases: テストを実行せずに spec を生成し、ケース宣言を build/acg/cases.csv に書く（:quality:checkCaseIds が集める）
// - -Pacg.runPending=true で保留中のケースも中身を実行し、結果を build/acg/pending-runs/ に、予想外に通ったものを build/acg/unexpected-pass/ に書く（夜間の強制実行、ADR-026）
// 適用したモジュールは quality/build.gradle.kts の acgCases にも足すこと
plugins {
    id("acg.jvm")
}

dependencies {
    "testImplementation"(project(":test-foundation"))
}

val testSourceSet = the<SourceSetContainer>()["test"]
val casesFile = layout.buildDirectory.file("acg/cases.csv")

val collectCases =
    tasks.register<JavaExec>("collectCases") {
        group = "verification"
        description = "テストのケース宣言（ID・種別・保留）を集めて build/acg/cases.csv に書く"
        classpath = testSourceSet.runtimeClasspath
        mainClass.set("me.tbsten.analysiscodegen.testfoundation.check.Main")
        jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
        val classDirs = testSourceSet.output.classesDirs
        val out = casesFile
        outputs.file(out)
        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf("collect", "--classes", classDirs.asPath, "--out", out.get().asFile.absolutePath)
            },
        )
    }

configurations.consumable("acgCasesElements") {
    attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named("acg-cases")) }
    outgoing.artifact(casesFile) { builtBy(collectCases) }
}

val runPending = providers.gradleProperty("acg.runPending").orElse("false")
val pendingResultsDir = layout.buildDirectory.dir("acg")

tasks.withType<Test>().configureEach {
    inputs.property("acg.runPending", runPending)
    systemProperty("acg.runPending", runPending.get())
    val resultsDir = pendingResultsDir.get().asFile
    systemProperty("acg.pendingResultsDir", resultsDir.absolutePath)
    // 前回の強制実行の結果が残らないよう、実行前に消す
    doFirst {
        resultsDir.resolve("pending-runs").deleteRecursively()
        resultsDir.resolve("unexpected-pass").deleteRecursively()
    }
}
