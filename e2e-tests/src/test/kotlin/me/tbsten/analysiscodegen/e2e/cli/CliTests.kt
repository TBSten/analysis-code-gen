package me.tbsten.analysiscodegen.e2e.cli

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.tbsten.analysiscodegen.e2e.support.Fixture
import me.tbsten.analysiscodegen.e2e.support.FixtureProject
import me.tbsten.analysiscodegen.e2e.support.contentsByPath
import me.tbsten.analysiscodegen.e2e.support.fixtureProject
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

// 5.22 CLI（CLI）。コマンドの形・終了コード・診断の書式は ADR-040。CLI は公開関数を持たないので、サブプロセスで流す
// 比べる相手は Gradle（F-*）が実際にエンジンへ渡したモデル（<workDir>/project-model.json、--info でパスが出る）

private const val CLI_MAIN = "me.tbsten.analysiscodegen.cli.MainKt"

private val json = Json { prettyPrint = true }

/** CLI を動かす classpath。e2e-tests のビルドが `:cli` の実行時 classpath を渡す（ADR-040）。 */
private val cliClasspath: String
    get() =
        System.getProperty("acg.cli.classpath")?.takeIf { it.isNotBlank() }
            ?: error("system property acg.cli.classpath が無い（e2e-tests のビルドが :cli の実行時 classpath を渡す。ADR-040）")

private data class CliRun(val exitCode: Int, val stdout: String, val stderr: String) {
    val stderrLines: List<String> get() = stderr.lines().filter { it.isNotBlank() }
}

private fun runCli(vararg args: String): CliRun {
    val java = File(System.getProperty("java.home"), "bin/java").absolutePath
    val process = ProcessBuilder(listOf(java, "-cp", cliClasspath, CLI_MAIN) + args).start()
    // 片方のパイプが詰まって止まらないよう、標準出力と標準エラーを並行して読む
    val out = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
    val err = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().readText() }
    check(process.waitFor(10, TimeUnit.MINUTES)) { "CLI が 10 分で終わらない: ${args.toList()}" }
    return CliRun(process.exitValue(), out.get(), err.get())
}

private val MODEL_LINE = Regex("""analysisCodeGen\[([^\]]+)]: model=(.+)$""", RegexOption.MULTILINE)

/** Gradle がエンジンに渡したモデル 1 つ（実行単位ごと）。 */
private class GradleModel(val unit: String, val file: File) {
    val root: JsonObject = Json.parseToJsonElement(file.readText()).jsonObject
    val rootDir: File get() = File(root.str("rootDir"))

    fun unitObject(name: String = unit): JsonObject =
        root
            .getValue("units")
            .jsonArray
            .map { it.jsonObject }
            .single { it.str("name") == name }

    /** Gradle が書いた生成物（`kotlin/…`・`java/…`・`resources/…` → 中身）。 */
    fun gradleOutputs(): Map<String, String> = outputsOf(unitObject().getValue("outputDirs").jsonObject, rootDir)
}

private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

private fun outputsOf(
    outputDirs: JsonObject,
    rootDir: File,
): Map<String, String> =
    listOf("kotlin", "java", "resources")
        .flatMap { kind ->
            val dir = File(outputDirs.str(kind)).let { if (it.isAbsolute) it else rootDir.resolve(it.path) }
            dir.contentsByPath().map { (path, content) -> "$kind/$path" to content }
        }.toMap()
        .toSortedMap()

/** フィクスチャを Native で [tasks] までビルドし、--info に出たモデルを実行単位ごとに集める。 */
private fun FixtureProject.buildAndCaptureModels(vararg tasks: String): List<GradleModel> {
    val output = build(*tasks, "--info").output
    val models = MODEL_LINE.findAll(output).map { GradleModel(it.groupValues[1], File(it.groupValues[2].trim())) }.toList()
    withClue("--info に analysisCodeGen[<unit>]: model=<パス> が出ていない（ADR-040）") { models.shouldNotBeEmpty() }
    return models.distinctBy { it.unit }
}

/**
 * [model] の出力先（outputDirs・stagingDir・cacheDir）だけを [into] に向け、[edit] で手を加えた JSON を書く。
 * Gradle の出力を上書きしないためと、CLI の出力だけを読むため。
 */
private fun GradleModel.rewritten(
    into: File,
    edit: (JsonObject) -> JsonObject = { it },
): Pair<File, JsonObject> {
    val units =
        root.getValue("units").jsonArray.map { u ->
            val o = u.jsonObject
            val name = o.str("name")
            JsonObject(
                o +
                    mapOf(
                        "outputDirs" to
                            JsonObject(
                                listOf("kotlin", "java", "resources").associateWith {
                                    JsonPrimitive(into.resolve("$name/out/$it").absolutePath)
                                },
                            ),
                        "stagingDir" to JsonPrimitive(into.resolve("$name/staging").absolutePath),
                        "cacheDir" to JsonPrimitive(into.resolve("$name/cache").absolutePath),
                    ),
            )
        }
    val edited = edit(JsonObject(root + ("units" to JsonArray(units))))
    val file = into.resolve("project-model.json").apply { writeText(json.encodeToString(JsonElement.serializer(), edited)) }
    return file to edited
}

private fun tempDir(prefix: String): File = Files.createTempDirectory(prefix).toFile()

/** CLI の出力を、Gradle が同じ単位に出したものと比べる。 */
private fun GradleModel.cliOutputShouldMatchGradle() {
    val expected = gradleOutputs()
    val dir = tempDir("acg-cli")
    val (modelFile, edited) = rewritten(dir)
    val run = runCli("generate", "--model", modelFile.absolutePath, "--unit", unit)
    withClue("unit=$unit\nstdout:\n${run.stdout}\nstderr:\n${run.stderr}") {
        run.exitCode shouldBe 0
        val actual =
            outputsOf(
                edited
                    .getValue("units")
                    .jsonArray
                    .map { it.jsonObject }
                    .single { it.str("name") == unit }
                    .getValue("outputDirs")
                    .jsonObject,
                rootDir,
            )
        // 不一致のとき読めるよう、パスの一覧 → ファイルごとの順に比べる
        actual.keys shouldBe expected.keys
        expected.forEach { (path, content) -> withClue(path) { actual[path] shouldBe content } }
    }
}

class CliDiffTest : DiffSpec({
    case(
        "CLI-01",
        "JVM のプロジェクトモデル（JSON）を渡して CLI で生成すると、Gradle の生成物と一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        val project = fixtureProject(Fixture.JVM)
        val models = project.buildAndCaptureModels("compileKotlin")
        models.map { it.unit } shouldContainAll listOf("main")
        models.single { it.unit == "main" }.gradleOutputs().shouldNotBeEmpty()
        models.forEach { it.cliOutputShouldMatchGradle() }
    }
})

class CliIntegrationTest : IntegrationSpec({
    case("CLI-02", "KMP のプロジェクトモデルでも CLI で生成できる", pending = PendingCase(Milestone.M2)) {
        // Linux で組める JVM 側まで（commonMain の SourceSet 単位と jvmMain の Compilation 単位）。iOS は mac の夜間で KMP-* が見る
        val project = fixtureProject(Fixture.KMP)
        val models = project.buildAndCaptureModels("compileKotlinJvm")
        models.map { it.unit } shouldContainAll listOf("commonMain", "jvmMain")
        models.forEach { it.cliOutputShouldMatchGradle() }
    }
})

/** F-JVM の main のモデル。CLI-03 のうちモデルが要るものは、これを書き換えて作る。 */
private fun jvmMainModel(): GradleModel = fixtureProject(Fixture.JVM).buildAndCaptureModels("compileKotlin").single { it.unit == "main" }

private val SUMMARY = Regex("""^analysisCodeGen: (\d+)/(\d+) units, (\d+) files, (\d+) errors, (\d+) warnings$""")

private fun JsonObject.withUnitEdited(
    unit: String,
    edit: (JsonObject) -> JsonObject,
): JsonObject =
    JsonObject(
        this +
            (
                "units" to
                    JsonArray(
                        getValue("units").jsonArray.map { u -> u.jsonObject.let { if (it.str("name") == unit) edit(it) else it } },
                    )
            ),
    )

class CliUnitTest : UnitSpec({
    case("CLI-03", "終了コードと診断の出力形式が仕様どおり（入力の誤りは 2）", pending = PendingCase(Milestone.M2)) {
        val dir = tempDir("acg-cli-03")

        // 引数の誤り: 使い方を出して 2
        runCli().let { r ->
            r.exitCode shouldBe 2
            r.stderr shouldContain "Usage:"
        }
        runCli("generate").let { r ->
            r.exitCode shouldBe 2
            r.stderr shouldContain "--model"
        }

        // モデルが無い・JSON として不正: 1 行目に原因（パス）を出して 2
        val missing = dir.resolve("missing.json")
        runCli("generate", "--model", missing.absolutePath).let { r ->
            r.exitCode shouldBe 2
            r.stderrLines.first() shouldContain missing.absolutePath
        }
        val broken = dir.resolve("broken.json").apply { writeText("{ not json") }
        runCli("generate", "--model", broken.absolutePath).let { r ->
            r.exitCode shouldBe 2
            r.stderrLines.first() shouldContain broken.absolutePath
        }

        val model = jvmMainModel()

        // 対応しない schemaVersion: 版を出して 2
        val (future, _) = model.rewritten(dir.resolve("future")) { JsonObject(it + ("schemaVersion" to JsonPrimitive(999))) }
        runCli("generate", "--model", future.absolutePath).let { r ->
            r.exitCode shouldBe 2
            r.stderrLines.first() shouldContain "schemaVersion"
            r.stderrLines.first() shouldContain "999"
        }

        // 無い実行単位: 実在する単位名を並べて 2
        val (ok, okJson) = model.rewritten(dir.resolve("ok"))
        runCli("generate", "--model", ok.absolutePath, "--unit", "noSuchUnit").let { r ->
            r.exitCode shouldBe 2
            r.stderrLines.first() shouldContain "noSuchUnit"
            okJson.getValue("units").jsonArray.forEach { r.stderr shouldContain it.jsonObject.str("name") }
        }

        // Processor の classpath のファイルが無い: 2
        val missingJar = dir.resolve("missing.jar")
        val (noJar, _) =
            model.rewritten(dir.resolve("no-jar")) {
                it.withUnitEdited("main") { u ->
                    JsonObject(
                        u +
                            (
                                "processors" to
                                    JsonArray(
                                        listOf(
                                            JsonObject(
                                                mapOf(
                                                    "component" to JsonPrimitive("example:missing:1.0"),
                                                    "classpath" to JsonArray(listOf(JsonPrimitive(missingJar.absolutePath))),
                                                ),
                                            ),
                                        ),
                                    )
                            ),
                    )
                }
            }
        runCli("generate", "--model", noJar.absolutePath).let { r ->
            r.exitCode shouldBe 2
            r.stderr shouldContain "missing.jar"
        }
    }

    case("CLI-03", "終了コードと診断の出力形式が仕様どおり（成功は 0、集計 1 行と JSON）", pending = PendingCase(Milestone.M2)) {
        val model = jvmMainModel()
        val (ok, _) = model.rewritten(tempDir("acg-cli-03"))

        // text: 標準出力の最後に集計 1 行。成功した単位数 = 全単位数、error 0
        runCli("generate", "--model", ok.absolutePath, "--unit", "main").let { r ->
            withClue(r.stderr) { r.exitCode shouldBe 0 }
            val summary = r.stdout.lines().last { it.isNotBlank() }
            summary shouldMatch SUMMARY
            val (succeeded, total, files, errors) = SUMMARY.matchEntire(summary)!!.destructured
            succeeded shouldBe total
            total shouldBe "1"
            (files.toInt() > 0) shouldBe true
            errors shouldBe "0"
        }

        // json: 標準出力に JSON 1 つ、標準エラーは空
        runCli("generate", "--model", ok.absolutePath, "--unit", "main", "--format", "json").let { r ->
            r.exitCode shouldBe 0
            r.stderr.isBlank() shouldBe true
            val root = Json.parseToJsonElement(r.stdout).jsonObject
            root.getValue("success").jsonPrimitive.boolean shouldBe true
            val unit = root.getValue("units").jsonArray.single().jsonObject
            unit.str("name") shouldBe "main"
            unit.getValue("success").jsonPrimitive.boolean shouldBe true
            unit.getValue("generatedFiles").jsonArray.shouldNotBeEmpty()
            unit.keys shouldContainAll listOf("rounds", "diagnostics")
        }
    }

    case("CLI-03", "終了コードと診断の出力形式が仕様どおり（生成の失敗は 1、1 行 1 診断）", pending = PendingCase(Milestone.M2)) {
        // TP-Bad の log-error（@BadTarget の宣言を指して logger.error）で、ファイルと行の付いた error を 1 つ出す
        val model = jvmMainModel()
        val sourceRoot =
            model.root
                .getValue("sourceSets")
                .jsonArray
                .map { it.jsonObject }
                .single { it.str("name") == "main" }
                .getValue("kotlinSourceRoots")
                .jsonArray
                .first()
                .jsonPrimitive.content
                .let { File(it).let { f -> if (f.isAbsolute) f else model.rootDir.resolve(it) } }
        sourceRoot.resolve("me/tbsten/analysiscodegen/tp/bad/Broken.kt").apply {
            parentFile.mkdirs()
            writeText(
                "package me.tbsten.analysiscodegen.tp.bad\n\nimport me.tbsten.analysiscodegen.tp.BadTarget\n\n@BadTarget\nclass Broken\n",
            )
        }
        val (bad, _) =
            model.rewritten(tempDir("acg-cli-03")) {
                it.withUnitEdited("main") { u ->
                    JsonObject(
                        u +
                            (
                                "options" to
                                    JsonObject(
                                        u.getValue("options").jsonObject +
                                            mapOf("tp.processors" to JsonPrimitive("Bad"), "tp.bad.modes" to JsonPrimitive("log-error")),
                                    )
                            ),
                    )
                }
            }

        runCli("generate", "--model", bad.absolutePath, "--unit", "main").let { r ->
            r.exitCode shouldBe 1
            // `<severity>: [<unit>][<processor>] <file>:<line>: <message>`
            val line = r.stderrLines.single { it.startsWith("error: ") }
            line shouldMatch Regex("""^error: \[main]\[[^\]]+] .*Broken\.kt:\d+: .+$""")
            val summary = r.stdout.lines().last { it.isNotBlank() }
            SUMMARY.matchEntire(summary)!!.destructured.let { (succeeded, total, _, errors) ->
                succeeded shouldBe "0"
                total shouldBe "1"
                errors shouldBe "1"
            }
        }

        runCli("generate", "--model", bad.absolutePath, "--unit", "main", "--format", "json").let { r ->
            r.exitCode shouldBe 1
            val root = Json.parseToJsonElement(r.stdout).jsonObject
            root.getValue("success").jsonPrimitive.boolean shouldBe false
            val diagnostic =
                root
                    .getValue("units")
                    .jsonArray
                    .single()
                    .jsonObject
                    .getValue("diagnostics")
                    .jsonArray
                    .map { it.jsonObject }
                    .single { it.str("severity") == "error" }
            diagnostic.str("file") shouldMatch Regex(""".*Broken\.kt$""")
            (diagnostic.getValue("line").jsonPrimitive.content.toInt() > 0) shouldBe true
            diagnostic.str("processor").isNotBlank() shouldBe true
        }
    }
})
