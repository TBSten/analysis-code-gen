plugins {
    id("acg.tests")
}

// ACG-46: CLI-01〜03 は CLI をサブプロセスで流す（ADR-040）。CLI の実行に要る AA 一式は、CLI の配布形を決めるときにここへ足す
val acgCliRuntime: Configuration by configurations.creating

dependencies {
    testImplementation(project(":analysis-code-gen-testing"))
    testImplementation(gradleTestKit())
    testImplementation(project(":test-processors:ksp")) // ACG-46: TST の Processor（ADR-027）。ACG-45: BRG・BIN
    testImplementation(project(":pbt")) // ACG-46: PBT-03〜05（ADR-022）
    testImplementation(project(":differential")) // ACG-46: XD（ADR-023）
    testImplementation(libs.kotlinpoet.ksp) // ACG-45: BRG-11
    // GCC-04（U）: ProjectBuilder で plugin を適用してタスクの入力を調べる。analysis-code-gen-gradle-plugin は AA に依存しない（ADR-012）
    testImplementation(project(":analysis-code-gen-gradle-plugin"))
    testImplementation(gradleApi())
    testImplementation(libs.kotlin.gradle.plugin)
    testImplementation(libs.kotlinx.serialization.json) // ACG-46: CLI のモデル JSON を書き換える
    acgCliRuntime(project(":analysis-code-gen-cli"))
    // TestEngine.AnalysisCodegen（BRG など）で AA Standalone を実際に動かす一式（ADR-060）。コンパイルには載せない
    testRuntimeOnly(libs.bundles.aa.runtime) { isTransitive = false }
}

// AA の *-for-ide と kotlin-compiler-embeddable は org.jetbrains.kotlin.cli.* を同じ名前で持つ。エンジンが for-ide 側を読むよう、
// AA の一式を実行時クラスパスの先頭に置く（ADR-060）。KSP2 経路（kct）は testing が別の ClassLoader で動かす（ADR-065）
val aaRuntimeFirst: Configuration by configurations.creating {
    isTransitive = false
}
dependencies { aaRuntimeFirst(libs.bundles.aa.runtime) }

tasks.withType<Test>().configureEach {
    // TestKit のビルドは plugin・エンジン・テスト用 Processor を build/fixture-repo から読む（ADR-027・060）
    dependsOn(":publishFixtureDeps")
    // F-* フィクスチャを複製して動かす補助（e2e.support、ADR-032）がリポジトリのルートを知るため
    systemProperty("acg.repoRoot", rootDir.absolutePath)
    // TestKit のフィクスチャが使う Kotlin（ADR-011）と Maven ミラー（クラウド環境向け、ADR-025）
    providers.gradleProperty("acg.kotlin").orNull?.let { systemProperty("acg.kotlin", it) }
    providers.gradleProperty("acg.mavenMirror").orNull?.let { systemProperty("acg.mavenMirror", it) }
    // ACG-46: CLI の実行時 classpath（ADR-040）
    jvmArgumentProviders += CliClasspathArgument(acgCliRuntime)
    classpath = aaRuntimeFirst + classpath
}

/** CLI の実行時 classpath を system property で渡す。構成キャッシュに載るよう、スクリプトを参照しないクラスにする。 */
class CliClasspathArgument(
    @get:Classpath val classpath: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-Dacg.cli.classpath=${classpath.asPath}")
}
