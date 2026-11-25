package me.tbsten.analysiscodegen.e2e.rw

import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.e2e.support.RealWorldSample
import me.tbsten.analysiscodegen.e2e.support.SampleEngine
import me.tbsten.analysiscodegen.e2e.support.publicAbi
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase

// 5.20 実用 Processor の E2E（RW）のうち種別 D（KSP2 との差分）のもの。固定サンプルでの一致を見る。ランダムな入力での一致は XD-*
// 比べ方: 生成物を「kotlin/… などの相対パス → 中身（LF）」の辞書順で並べて丸ごと比べる。パスと順序の正規化は generatedFiles が行う

/** サンプルを K と [engine] でビルドし、[sourceSets] の生成物が正規化後に一致する。 */
private fun RealWorldSample.generatedShouldMatchKsp2(
    engine: SampleEngine = SampleEngine.Bridge,
    task: String = "assemble",
    module: String = "",
    sourceSets: List<String> = listOf("main"),
) {
    val gradleTask = if (module.isEmpty()) task else ":$module:$task"
    build(SampleEngine.Ksp2, "clean", gradleTask)
    val expected = sourceSets.associateWith { generated(SampleEngine.Ksp2, it, module) }
    build(engine, "clean", gradleTask)
    val actual = sourceSets.associateWith { generated(engine, it, module) }

    expected.values.forEach { it.shouldNotBeEmpty() }
    // 不一致のとき差分が読めるよう、SourceSet ごと・ファイルごとに比べる
    for (sourceSet in sourceSets) {
        actual.getValue(sourceSet).keys shouldBe expected.getValue(sourceSet).keys
        for ((path, content) in expected.getValue(sourceSet)) {
            actual.getValue(sourceSet)[path] shouldBe content
        }
    }
}

class RealWorldDiffTest : DiffSpec({
    case("RW-CRM-B2", "cream.kt（Bridge）: 生成物が KSP2 と一致する（パスと順序は正規化）", pending = PendingCase(Milestone.M2)) {
        RealWorldSample.open("CRM", "jvm").generatedShouldMatchKsp2()
    }

    case("RW-MSH-B2", "Moshi（Bridge）: 生成物が KSP2 と一致する", pending = PendingCase(Milestone.M2)) {
        // Moshi は proguard ルール（resources/META-INF/proguard/…）も生成する。リソースも比べる対象に入っている
        RealWorldSample.open("MSH", "jvm").generatedShouldMatchKsp2()
    }

    case("RW-KIN-B2", "kotlin-inject（Bridge）: 生成物が KSP2 と一致する", pending = PendingCase(Milestone.M2)) {
        RealWorldSample.open("KIN", "jvm").generatedShouldMatchKsp2()
    }

    case(
        "RW-KIN-B4",
        "kotlin-inject（Bridge）: KMP のサンプル（JVM + iOS + JS）が Compatible モードで KSP2 と同じ結果になる",
        pending = PendingCase(Milestone.M3),
    ) {
        // mac ランナーで流す（iOS）。KSP2 の KMP は Compilation 単位（kspJvm・kspIosArm64・kspJs）に出すので、
        // Bridge の Compatible モードも Compilation 単位の出力を比べる（ADR-016・017）
        RealWorldSample.open("KIN", "kmp").generatedShouldMatchKsp2(
            task = "allTests",
            sourceSets = listOf("jvmMain", "iosSimulatorArm64Main", "jsMain"),
        )
    }

    case("RW-KOI-B2", "Koin Annotations（Bridge）: 生成物が KSP2 と一致する", pending = PendingCase(Milestone.M2)) {
        RealWorldSample.open("KOI", "jvm").generatedShouldMatchKsp2()
    }

    case("RW-ROM-B2", "Room（Bridge）: 生成物が KSP2 と一致する", pending = PendingCase(Milestone.M3)) {
        // Room はスキーマ JSON（room.schemaLocation）も出す。サンプルはスキーマを build/ 以下に出し、生成物と一緒に比べる
        val sample = RealWorldSample.open("ROM", "android")
        sample.generatedShouldMatchKsp2(task = "assembleDebug", module = "app", sourceSets = listOf("debug"))
        val schemas = { e: SampleEngine ->
            sample.projectDir
                .resolve("app/build/schemas-${e.property}")
                .walkTopDown()
                .filter { it.isFile }
                .map { it.readText() }
                .toList()
        }
        schemas(SampleEngine.Bridge) shouldBe schemas(SampleEngine.Ksp2)
    }

    case("RW-DAG-B3", "Dagger / Hilt（Bridge）: 生成物が KSP2 と一致する", pending = PendingCase(Milestone.M3)) {
        RealWorldSample.open("DAG", "jvm").generatedShouldMatchKsp2()
        RealWorldSample
            .open(
                "DAG",
                "hilt-android",
            ).generatedShouldMatchKsp2(task = "assembleDebug", module = "app", sourceSets = listOf("debug"))
    }

    case(
        "RW-DAG-N2",
        "Dagger / Hilt（Native）: 生成物が KSP2 と三者差分テストの基準（XD-05・XD-12）で一致する",
        pending = PendingCase(Milestone.Optional),
    ) {
        val sample = RealWorldSample.open("DAG", "jvm")
        // 段階 1（XD-05）: コンパイル結果の公開 ABI と実行時テストの結果が一致する
        sample.build(SampleEngine.Ksp2, "clean", "test")
        val abiK = publicAbi(sample.projectDir.resolve("build/classes"))
        val testsK = sample.testReport()
        sample.build(SampleEngine.Native, "clean", "test")
        publicAbi(sample.projectDir.resolve("build/classes")) shouldBe abiK
        sample.testReport() shouldBe testsK
        // 段階 2（XD-12）: 生成コードが正規化後に一致する
        sample.generatedShouldMatchKsp2(engine = SampleEngine.Native)
    }
})
