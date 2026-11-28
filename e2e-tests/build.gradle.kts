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

// ---- 実用 Processor とその実行時ライブラリ（ADR-037・ACG-68）
// XD-* とカタログの振り分け（verifyCatalog）は、元の Processor の KSP 版を jar から読み、利用者のコードは実行時ライブラリを classpath に足して
// コンパイルする。版は processors.toml（ADR-019）だけを正とし、ここでは読むだけにする
val processorVersions: Map<String, String> =
    rootDir
        .resolve("fixtures/processors/processors.toml")
        .readText()
        .split("[[processor]]")
        .drop(1)
        .associate { block ->
            fun value(key: String) =
                Regex("""(?m)^$key\s*=\s*"([^"]*)"""")
                    .find(block)
                    ?.groupValues
                    ?.get(1)
                    .orEmpty()
            value("id") to value("version")
        }

/** Processor 1 つの KSP 版の jar（[processor]）と、カタログの入力がコンパイルに使う実行時ライブラリ（[runtime]）。 */
class RealProcessorArtifacts(
    val processor: List<String>,
    val runtime: List<String> = emptyList(),
)

fun v(id: String) = processorVersions.getValue(id)

// koin-core は koin-ksp-compiler 2.3.1 の pom の版（fixtures の acg.fixture-settings と同じ）。
// カタログにまだパターンが無い KIN・DAG・ROM も、XD-02 の Processor の組（Arb.processorSet）に入るので KSP 版の jar だけ渡す
val realProcessorArtifacts: Map<String, RealProcessorArtifacts> =
    mapOf(
        "CRM" to
            RealProcessorArtifacts(
                listOf("me.tbsten.cream:cream-ksp:${v("CRM")}"),
                listOf("me.tbsten.cream:cream-runtime:${v("CRM")}"),
            ),
        "MSH" to
            RealProcessorArtifacts(
                listOf("com.squareup.moshi:moshi-kotlin-codegen:${v("MSH")}"),
                listOf("com.squareup.moshi:moshi:${v("MSH")}"),
            ),
        "KOI" to
            RealProcessorArtifacts(
                listOf("io.insert-koin:koin-ksp-compiler:${v("KOI")}"),
                listOf("io.insert-koin:koin-annotations:${v("KOI")}", "io.insert-koin:koin-core:4.1.0"),
            ),
        "KIN" to RealProcessorArtifacts(listOf("me.tatarka.inject:kotlin-inject-compiler-ksp:${v("KIN")}")),
        "DAG" to RealProcessorArtifacts(listOf("com.google.dagger:dagger-compiler:${v("DAG")}")),
        "ROM" to RealProcessorArtifacts(listOf("androidx.room:room-compiler:${v("ROM")}")),
    )

/** JVM の実行時の属性（KMP のライブラリから JVM の variant を選ぶため）を testRuntimeClasspath から写した、解決専用の構成。 */
fun jvmResolvable(name: String): Configuration =
    configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        val source = configurations.testRuntimeClasspath.get().attributes
        attributes {
            source.keySet().forEach { key ->
                @Suppress("UNCHECKED_CAST")
                attribute(key as Attribute<Any>, source.getAttribute(key)!!)
            }
        }
    }

val realProcessorClasspaths: Map<String, Pair<Configuration, Configuration>> =
    realProcessorArtifacts.mapValues { (id, artifacts) ->
        val processor =
            jvmResolvable("acgRealProcessor$id").apply {
                // KSP の API と Kotlin の標準ライブラリはテストの JVM のものを使う（親の ClassLoader から読む）
                exclude(group = "com.google.devtools.ksp")
                exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            }
        val runtime = jvmResolvable("acgCatalogRuntime$id")
        artifacts.processor.forEach { dependencies.add(processor.name, it) }
        artifacts.runtime.forEach { dependencies.add(runtime.name, it) }
        processor to runtime
    }

/** `acg.realProcessors.<ID>`（Processor の jar）と `acg.catalogClasspath.<ID>`（実行時ライブラリ）を渡す。 */
class RealProcessorArguments(
    @get:Input val id: String,
    @get:Classpath val processor: FileCollection,
    @get:Classpath val runtime: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf("-Dacg.realProcessors.$id=${processor.asPath}", "-Dacg.catalogClasspath.$id=${runtime.asPath}")
}

val realProcessorArguments = realProcessorClasspaths.map { (id, c) -> RealProcessorArguments(id, c.first, c.second) }

tasks.withType<Test>().configureEach { jvmArgumentProviders += realProcessorArguments }

// カタログの全パターンを K（KSP2）で動かして EXPECT と比べ、結果を TSV に書く（ACG-68、ADR-072）。
// 書いた TSV は `python3 quality/catalog/scripts/catalog.py apply-k <TSV>` で各パターンの STATUS に反映する。
// 引数: -Pacg.catalog.shard=<i>/<n>（分けて並列に動かす）、-Pacg.catalog.ids=<ID,ID>（一部だけ）、-Pacg.catalog.out=<TSV>、-Pacg.catalog.verbose=true
tasks.register<JavaExec>("verifyCatalog") {
    group = "verification"
    description = "入力パターンカタログを KSP2 で動かして verified / rejected に振り分ける（ADR-020 の 6 項）"
    dependsOn("testClasses")
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    classpath = aaRuntimeFirst + sourceSets["test"].runtimeClasspath
    mainClass.set("me.tbsten.analysis.code.gen.e2e.xd.CatalogVerifySupportKt")
    systemProperty("acg.repoRoot", rootDir.absolutePath)
    jvmArgumentProviders += realProcessorArguments
    maxHeapSize = "3g"
    // -Pacg.catalog.verbose=true で K の診断をすべて出す（rejected の原因を調べるとき）
    providers.gradleProperty("acg.catalog.verbose").orNull?.let { systemProperty("acg.catalog.verbose", it) }
    val shard = providers.gradleProperty("acg.catalog.shard").orElse("0/1")
    val ids = providers.gradleProperty("acg.catalog.ids").orElse("")
    val out =
        providers
            .gradleProperty(
                "acg.catalog.out",
            ).orElse(layout.buildDirectory.file("acg/catalog-k.tsv").map { it.asFile.absolutePath })
    argumentProviders += CommandLineArgumentProvider { listOf(shard.get(), ids.get(), out.get()) }
}
