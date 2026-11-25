package me.tbsten.analysis.code.gen.kspcompatibilitytests.kspt

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import java.io.File
import java.nio.file.Files

/** 取り込んだ KSP 公式テスト 1 件（`kspt/cases.csv` の 1 行。porting/ksp/import_kspt.py が書く）。 */
data class KsptCase(
    val id: String,
    val method: String,
    val testData: String,
    val milestone: Milestone,
    val category: String,
    /** `positive`（期待値と一致）か `throwing`（InternalKSPException を投げる）。 */
    val mode: String,
) {
    val title: String get() = "KSP 公式テスト $method（$testData）が 4 つの API 機能の組すべてで期待どおりになる"
}

/** KSPT を流す実装。 */
enum class KsptEngine {
    /** 公式 KSP2 2.3.14（期待値の出どころ。ここで通らないものは取り込まない）。 */
    Ksp2,

    /** analysisCodeGen の KSP Bridge。エンジン（P2）ができるまで NotImplementedError。 */
    Bridge,
}

object KsptCases {
    val all: List<KsptCase> by lazy {
        resource("kspt/cases.csv")
            .lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val c = line.split(',')
                KsptCase(c[0], c[1], c[2], Milestone.ofLabel(c[3]) ?: error("M が不正: $line"), c[4], c[5])
            }
    }

    /**
     * Bridge で保留を外したケース（`kspt/bridge-enabled.txt`、1 行 1 ID）。
     * 足すだけにする（R-01）。消すと :quality:checkCaseIds が落ちる。
     */
    val bridgeEnabled: Set<String> by lazy {
        resource("kspt/bridge-enabled.txt")
            .lines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /** 期待値が JDK 17 の標準ライブラリに依存するケース（JDK 21 ではメソッドが増える）。 */
    val jdk17Only: Set<String> = setOf("KSPT-AllFunctions_kt_inherits_java")

    fun testData(case: KsptCase): KsptTestData = KsptTestData.parse(case.testData, resource("kspt/testData/${case.testData}"))

    /** 4 つの API 機能の組すべてで、期待値どおりになる（throwing のときは例外が出る）ことを確かめる。 */
    fun assertPasses(
        case: KsptCase,
        engine: KsptEngine,
        features: List<KsptFeatures> = KsptFeatures.ALL,
    ) {
        val data = testData(case)
        val workDir = Files.createTempDirectory("kspt-${case.method}").toFile()
        try {
            for (f in features) {
                withClue("${case.id} [$f]") {
                    val actual = run(engine, data, f, File(workDir, "$f".replace(Regex("[^A-Za-z]"), "_")))
                    actual shouldBe data.expected.getValue(f)
                }
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun run(
        engine: KsptEngine,
        data: KsptTestData,
        features: KsptFeatures,
        workDir: File,
    ): String =
        when (engine) {
            KsptEngine.Ksp2 -> Ksp2KsptRunner.run(data, features, workDir)
            KsptEngine.Bridge -> throw NotImplementedError("KSP Bridge はエンジン（P2）ができてから動かす")
        }

    private fun resource(path: String): String =
        checkNotNull(KsptCases::class.java.classLoader.getResource(path)) { "リソースが無い: $path" }.readText()
}
