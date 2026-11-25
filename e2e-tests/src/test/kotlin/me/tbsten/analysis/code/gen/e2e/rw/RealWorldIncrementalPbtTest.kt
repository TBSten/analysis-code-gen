package me.tbsten.analysis.code.gen.e2e.rw

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.e2e.support.RealWorldSample
import me.tbsten.analysis.code.gen.e2e.support.SampleEngine
import me.tbsten.analysis.code.gen.e2e.support.copyProject
import me.tbsten.analysis.code.gen.e2e.support.generatedFiles
import me.tbsten.analysis.code.gen.e2e.support.gradleArgs
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PbtSpec
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

// RW-ALL-I1: 実用 Processor のサンプルに代表的な変更履歴（seed 1〜20 の 20 履歴）を当て、増分の結果がクリーンビルドと一致する
// 履歴は決定的（seed → 操作列）。ランダムな履歴での一致は XD-09・10 が受け持つ
// サンプルの決まり（ADR-037）: src/main/kotlin の 1 ファイルに注釈付きのトップレベル宣言 1 つ・ファイル名 = 宣言名。
// このため「ファイルを写して宣言名を変える」操作でコンパイルが壊れない

private const val HISTORIES = 20
private const val STEPS_PER_HISTORY = 5

private sealed interface Edit {
    /** 既存のファイルを写し、宣言名に接尾辞を付けた新しいファイルを足す（注釈ごと増えるので生成物も増える）。 */
    data class Duplicate(val from: String, val suffix: String) : Edit

    /** ファイルの末尾にトップレベル関数を足す（宣言の形は変えず、ファイルの中身だけ変える）。 */
    data class Touch(val file: String, val marker: String) : Edit

    /** この履歴で足したファイルを消す（元のファイルは実行時テストが参照するので消さない）。 */
    data class RemoveAdded(val file: String) : Edit
}

private fun sourceRoot(projectDir: File) = projectDir.resolve("src/main/kotlin")

/** seed から操作列を作る。対象の選び方は「その時点で存在するファイル」に依存するので、適用しながら決める。 */
private fun applyHistory(
    projectDir: File,
    seed: Int,
    afterEach: (Edit) -> Unit,
) {
    val random = Random(seed)
    val root = sourceRoot(projectDir)
    val originals =
        root
            .walkTopDown()
            .filter {
                it.isFile && it.extension == "kt"
            }.map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
    check(originals.isNotEmpty()) { "サンプルに src/main/kotlin のソースが無い: $projectDir" }
    val added = mutableListOf<String>()
    repeat(STEPS_PER_HISTORY) { step ->
        val edit =
            when (if (added.isEmpty()) random.nextInt(2) else random.nextInt(3)) {
                0 -> Edit.Duplicate(originals[random.nextInt(originals.size)], "H${seed}S$step")
                1 -> Edit.Touch((originals + added).let { it[random.nextInt(it.size)] }, "h${seed}s$step")
                else -> Edit.RemoveAdded(added[random.nextInt(added.size)])
            }
        when (edit) {
            is Edit.Duplicate -> {
                val source = root.resolve(edit.from)
                val name = source.nameWithoutExtension
                val target = source.resolveSibling("$name${edit.suffix}.kt")
                target.writeText(source.readText().replace(Regex("\\b$name\\b"), "$name${edit.suffix}"))
                added += target.relativeTo(root).invariantSeparatorsPath
            }
            is Edit.Touch -> root.resolve(edit.file).appendText("\n\npublic fun ${edit.marker}(): Int = ${edit.marker.length}\n")
            is Edit.RemoveAdded -> {
                root.resolve(edit.file).delete()
                added -= edit.file
            }
        }
        afterEach(edit)
    }
}

/** 履歴を当てるサンプル。JVM のサンプルが無い Room は Android サンプルの :app で見る。 */
private data class HistoryTarget(
    val processorId: String,
    val sample: String = "jvm",
    val module: String = "",
    val compileTask: String = "compileKotlin",
    val sourceSet: String = "main",
)

private val ROOM_ANDROID =
    HistoryTarget("ROM", sample = "android", module = "app", compileTask = ":app:compileDebugKotlin", sourceSet = "debug")

/** [engine] でサンプルに 20 履歴を当て、各履歴の最後で「増分の生成物 = 同じ状態のクリーンビルドの生成物」を確かめる。 */
private fun incrementalShouldEqualClean(
    target: HistoryTarget,
    engine: SampleEngine,
) {
    for (seed in 1..HISTORIES) {
        val incremental = RealWorldSample.open(target.processorId, target.sample)
        val moduleDir = incremental.projectDir.resolve(target.module)
        incremental.build(engine, target.compileTask)
        applyHistory(moduleDir, seed) { incremental.build(engine, target.compileTask) }
        val incrementalOutput = generatedFiles(moduleDir, engine, target.sourceSet)

        // 最終状態のソースだけを写した別ディレクトリで、クリーンにビルドする
        val cleanDir = Files.createTempDirectory("acg-rw-clean").toFile()
        copyProject(incremental.projectDir, cleanDir)
        cleanDir
            .walkTopDown()
            .filter { it.isDirectory && (it.name == "build" || it.name == ".gradle") }
            .toList()
            .forEach { it.deleteRecursively() }
        GradleRunner
            .create()
            .withProjectDir(cleanDir)
            .withArguments(listOf(target.compileTask) + gradleArgs(engine))
            .forwardOutput()
            .build()

        withClue("${target.processorId} / $engine / seed=$seed") {
            incrementalOutput shouldBe generatedFiles(cleanDir.resolve(target.module), engine, target.sourceSet)
        }
    }
}

class RealWorldIncrementalPbtTest : PbtSpec({
    // Native 版が必須の Processor（M2）
    case(
        "RW-ALL-I1",
        "Native 版（cream.kt・Moshi・Koin）: 代表的な変更履歴（20 履歴）のあとの増分結果がクリーンビルドと一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        listOf("CRM", "MSH", "KOI").forEach { incrementalShouldEqualClean(HistoryTarget(it), SampleEngine.Native) }
    }

    // Bridge 版（M3）。同じケース ID の 2 本目。マイルストーンは一覧の M 列の最初の値（M2）に合わせる決まり（ADR-030）
    case(
        "RW-ALL-I1",
        "Bridge 版（6 つすべて。Room は Android サンプル）: 代表的な変更履歴（20 履歴）のあとの増分結果がクリーンビルドと一致する",
        pending = PendingCase(Milestone.M2),
    ) {
        (listOf("CRM", "MSH", "KOI", "KIN", "DAG").map(::HistoryTarget) + ROOM_ANDROID).forEach {
            incrementalShouldEqualClean(it, SampleEngine.Bridge)
        }
    }
})
