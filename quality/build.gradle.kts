// テストケース一覧とテストの整合チェック（ACG-37、ADR-030）
// - checkCaseIds（check に含む）: 一覧とテストのずれ・保留への逆戻り・状態ファイルの古さを検査する
// - updateCaseStatus: quality/case-status.csv（進捗レポートが読むケースの状態）を書き直す
plugins {
    java
}

val acgCases = configurations.dependencyScope("acgCases")
val acgCasesFiles =
    configurations.resolvable("acgCasesFiles") {
        extendsFrom(acgCases.get())
        attributes { attribute(Usage.USAGE_ATTRIBUTE, objects.named("acg-cases")) }
    }

dependencies {
    runtimeOnly(project(":test-foundation"))
    // ケース ID 付きテストを持つモジュール（acg.tests を適用したもの）をすべて並べる
    acgCases(project(":e2e-tests"))
    acgCases(project(":pbt"))
    acgCases(project(":differential"))
}

val baseRef = providers.gradleProperty("acg.caseCheck.baseRef").orElse("origin/main")
val requireAll = providers.gradleProperty("acg.caseCheck.requireAllCases").orElse("false")
val repoDir = rootDir.absolutePath
val qualityDir = layout.projectDirectory

fun JavaExec.configureCaseCheck(write: Boolean) {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("me.tbsten.analysiscodegen.testfoundation.check.Main")
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    // 構成キャッシュのため、ラムダはスクリプトのトップレベルを参照せずローカル値だけを掴む
    val manifests = files(acgCasesFiles)
    val inventory = qualityDir.file("test-cases.csv").asFile.absolutePath
    val status = qualityDir.file("case-status.csv").asFile.absolutePath
    val exceptions = qualityDir.file("case-exceptions.csv").asFile.absolutePath
    val ref = baseRef
    val all = requireAll
    val repo = repoDir
    inputs.files(manifests)
    inputs.file(inventory)
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "check",
                "--manifests", manifests.asPath,
                "--inventory", inventory,
                "--status", status,
                "--exceptions", exceptions,
                "--base-ref", ref.get(),
                "--repo", repo,
                "--require-all", all.get(),
                "--write", write.toString(),
            )
        },
    )
}

val checkCaseIds =
    tasks.register<JavaExec>("checkCaseIds") {
        description = "テストケース一覧とテストのケース ID・状態の整合を検査する"
        configureCaseCheck(write = false)
    }

tasks.register<JavaExec>("updateCaseStatus") {
    description = "quality/case-status.csv をテストの宣言から書き直す"
    configureCaseCheck(write = true)
}

tasks.named("check") { dependsOn(checkCaseIds) }
