package me.tbsten.analysiscodegen.e2e.support

import me.tbsten.analysiscodegen.testing.gradle.GradleFixture
import me.tbsten.analysiscodegen.testing.gradle.gradleFixture
import me.tbsten.analysiscodegen.testprocessors.TpAnnotationsSource
import java.io.File
import java.util.Properties

// ACG-45 の E2E（INC・OUT・FAIL・GCC・BRG・BINC・MIX・BKMP・BIN・HYB）が使う補助（ADR-034）。
// ACG-44 の support（fixtureProject など）が入ったら、重なる部分はそちらに寄せる。
// Bridge の道（me.tbsten.analysiscodegen.ksp-bridge）を使うケースの保留を外すときは、ksp-bridge-gradle-plugin を publishFixtureDeps に足す。

/** どの道でフィクスチャを動かすか。 */
enum class Acg45Engine {
    /** 公開 DSL + TP の Native 版。 */
    Native,

    /** 公開 DSL + ksp-bridge + TP の KSP 版。 */
    Bridge,

    /** 公式 KSP2 + TP の KSP 版。差分の基準（K）。 */
    Ksp2,
}

/** リポジトリのルートと、フィクスチャが読む成果物の場所（ADR-027 の build/fixture-repo）。 */
object Acg45Repo {
    val root: File by lazy {
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "gradle/libs.versions.toml").isFile && File(it, "fixtures/projects").isDirectory }
            ?: error("analysis-code-gen のリポジトリのルートが見つからない")
    }

    val acgVersion: String by lazy {
        Properties().also { p -> File(root, "gradle.properties").inputStream().use(p::load) }.getProperty("acg.version")
    }

    val kspVersion: String by lazy {
        Regex("""(?m)^ksp\s*=\s*"([^"]+)"""").find(File(root, "gradle/libs.versions.toml").readText())!!.groupValues[1]
    }

    val fixtureRepo: File get() = File(root, "build/fixture-repo")

    fun coordinate(artifact: String): String = "me.tbsten.analysiscodegen:$artifact:$acgVersion"
}

/**
 * kotlin("jvm") 1 モジュールのフィクスチャ。TP のアノテーションを置き、[engine] の道で [processors]（`tp.processors`）を動かす。
 *
 * - [dsl] は `analysisCodeGen { }` の中に足す行（[Acg45Engine.Ksp2] では `ksp { }` の中）
 * - [probeOut] を渡すと TP-Probe の記録先を `tp.probe.out` に設定する
 */
fun acg45Project(
    engine: Acg45Engine = Acg45Engine.Native,
    processors: String = "Mapper,Registry",
    dsl: String = "",
    extraBuild: String = "",
    probeOut: File? = null,
    block: GradleFixture.() -> Unit = {},
): GradleFixture =
    gradleFixture {
        val repo = "maven { url = uri(\"${Acg45Repo.fixtureRepo.toURI()}\") }"
        extraPluginRepositories += repo
        extraRepositories += repo
        val args =
            buildList {
                add("arg(\"tp.processors\", \"$processors\")")
                probeOut?.let { add("arg(\"tp.probe.out\", \"${it.absolutePath.replace("\\", "/")}\")") }
            }.joinToString("\n    ")
        val v = Acg45Repo.acgVersion
        buildScript =
            when (engine) {
                Acg45Engine.Native ->
                    """
                    plugins {
                        kotlin("jvm") version "{{kotlinVersion}}"
                        id("me.tbsten.analysiscodegen") version "$v"
                    }

                    analysisCodeGen {
                        $args
                        $dsl
                        sourceSets { main { processor("${Acg45Repo.coordinate("test-processors-native")}") } }
                    }
                    """.trimIndent()
                Acg45Engine.Bridge ->
                    """
                    plugins {
                        kotlin("jvm") version "{{kotlinVersion}}"
                        id("me.tbsten.analysiscodegen.ksp-bridge") version "$v"
                    }

                    analysisCodeGen {
                        $args
                        $dsl
                        sourceSets { main { processor("${Acg45Repo.coordinate("test-processors-ksp")}") } }
                    }
                    """.trimIndent()
                Acg45Engine.Ksp2 ->
                    """
                    plugins {
                        kotlin("jvm") version "{{kotlinVersion}}"
                        id("com.google.devtools.ksp") version "${Acg45Repo.kspVersion}"
                    }

                    ksp {
                        $args
                        $dsl
                    }

                    dependencies {
                        ksp("${Acg45Repo.coordinate("test-processors-ksp")}")
                    }
                    """.trimIndent()
            } + if (extraBuild.isBlank()) "" else "\n\n" + extraBuild.trimIndent()
        file("src/main/kotlin/me/tbsten/analysiscodegen/tp/${TpAnnotationsSource.FILE_NAME}", TpAnnotationsSource.content)
        block()
    }

/** 生成物のルート（`kotlin/`・`java/`・`resources/` の親）。公開 DSL は ADR-016、KSP2 は `build/generated/ksp/main`。 */
fun GradleFixture.generatedRoot(
    engine: Acg45Engine,
    unit: String = "main",
): File =
    when (engine) {
        Acg45Engine.Ksp2 -> projectDir.resolve("build/generated/ksp/$unit")
        else -> projectDir.resolve("build/generated/analysisCodeGen/$unit")
    }

/** 生成物を「ルートからの相対パス → 中身」で返す（辞書順）。無ければ空。 */
fun GradleFixture.generatedFiles(
    engine: Acg45Engine,
    unit: String = "main",
): Map<String, String> {
    val root = generatedRoot(engine, unit)
    if (!root.isDirectory) return emptyMap()
    return root
        .walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(root).invariantSeparatorsPath to it.readText() }
        .toSortedMap()
}

/** 生成物のファイル名（パスの最後）の集合。 */
fun GradleFixture.generatedNames(
    engine: Acg45Engine,
    unit: String = "main",
): Set<String> = generatedFiles(engine, unit).keys.map { it.substringAfterLast('/') }.toSet()

/** analysisCodeGen のタスク名（ADR-016）。kotlin("jvm") の main は `analysisCodeGenMain`。KSP2 は `kspKotlin`。 */
fun codegenTask(
    engine: Acg45Engine,
    unit: String = "main",
): String =
    when (engine) {
        Acg45Engine.Ksp2 -> ":kspKotlin"
        else -> ":analysisCodeGen" + unit.replaceFirstChar { it.uppercase() }
    }

/** TP-Probe の記録（JSON Lines）を 1 行ずつ返す。 */
fun readProbe(file: File): List<String> = if (file.isFile) file.readLines().filter { it.isNotBlank() } else emptyList()

/** TP-Probe の 1 行から、配列のフィールド（`newFiles` など）を取り出す。値は JSON の文字列のまま（エスケープは戻さない）。 */
fun probeArray(
    line: String,
    field: String,
): List<String> {
    val body = Regex("\"$field\":\\[([^\\]]*)]").find(line)?.groupValues?.get(1) ?: return emptyList()
    return Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(body).map { it.groupValues[1] }.toList()
}

/** TP-Probe の 1 行から、文字列か数値のフィールドを取り出す。 */
fun probeValue(
    line: String,
    field: String,
): String? = Regex("\"$field\":(\"((?:[^\"\\\\]|\\\\.)*)\"|-?\\d+)").find(line)?.let { it.groupValues[2].ifEmpty { it.groupValues[1] } }

/** `@Mapped` と `@Registered` を付けた data class 1 つのソース。 */
fun mappedClass(
    name: String,
    pkg: String = "com.example",
    props: String = "val id: Int",
): String =
    """
    package $pkg

    import me.tbsten.analysiscodegen.tp.Mapped
    import me.tbsten.analysiscodegen.tp.Registered

    @Mapped
    @Registered
    data class $name($props)
    """.trimIndent()
