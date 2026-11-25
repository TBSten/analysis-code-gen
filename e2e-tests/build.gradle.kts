plugins {
    id("acg.tests")
}

// ACG-46: CLI-01〜03 は CLI をサブプロセスで流す（ADR-040）。CLI の実行に要る AA 一式は、CLI の配布形を決めるときにここへ足す
val acgCliRuntime: Configuration by configurations.creating

dependencies {
    testImplementation(project(":testing"))
    testImplementation(gradleTestKit())
    testImplementation(project(":test-processors:ksp")) // ACG-46: TST の Processor（ADR-027）。ACG-45: BRG・BIN
    testImplementation(project(":pbt")) // ACG-46: PBT-03〜05（ADR-022）
    testImplementation(project(":differential")) // ACG-46: XD（ADR-023）
    testImplementation(libs.kotlinpoet.ksp) // ACG-45: BRG-11
    // GCC-04（U）: ProjectBuilder で plugin を適用してタスクの入力を調べる。gradle-plugin は AA に依存しない（ADR-012）
    testImplementation(project(":gradle-plugin"))
    testImplementation(gradleApi())
    testImplementation(libs.kotlin.gradle.plugin)
    testImplementation(libs.kotlinx.serialization.json) // ACG-46: CLI のモデル JSON を書き換える
    acgCliRuntime(project(":cli"))
}

tasks.withType<Test>().configureEach {
    // F-* フィクスチャを複製して動かす補助（e2e.support、ADR-032）がリポジトリのルートを知るため
    systemProperty("acg.repoRoot", rootDir.absolutePath)
    // TestKit のフィクスチャが使う Kotlin（ADR-011）と Maven ミラー（クラウド環境向け、ADR-025）
    providers.gradleProperty("acg.kotlin").orNull?.let { systemProperty("acg.kotlin", it) }
    providers.gradleProperty("acg.mavenMirror").orNull?.let { systemProperty("acg.mavenMirror", it) }
    // ACG-46: CLI の実行時 classpath（ADR-040）
    jvmArgumentProviders += CliClasspathArgument(acgCliRuntime)
}

/** CLI の実行時 classpath を system property で渡す。構成キャッシュに載るよう、スクリプトを参照しないクラスにする。 */
class CliClasspathArgument(
    @get:Classpath val classpath: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-Dacg.cli.classpath=${classpath.asPath}")
}
