package me.tbsten.analysis.code.gen.e2e.rw

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import me.tbsten.analysis.code.gen.e2e.support.RealWorldSample
import me.tbsten.analysis.code.gen.e2e.support.SampleEngine
import me.tbsten.analysis.code.gen.e2e.support.TestReport
import me.tbsten.analysis.code.gen.e2e.support.UpstreamSuite
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import org.gradle.testkit.runner.TaskOutcome

// 5.20 実用 Processor の E2E（RW）のうち種別 E のもの。サンプルの置き場と切り替えは ADR-037
// サンプル: fixtures/real-world/<processor>/<sample>。どれも生成コードを使う実行時テストを src/test に持つ

/** サンプルを [engine] でビルドし、実行時テストが 1 件以上走って全部通ることを確かめる。 */
private fun RealWorldSample.shouldPassRuntimeTests(
    engine: SampleEngine,
    module: String = "",
    testTask: String = "test",
) {
    val task = if (module.isEmpty()) testTask else ":$module:$testTask"
    val result = build(engine, "clean", task)
    result.task(":${task.removePrefix(":")}")?.outcome shouldBe TaskOutcome.SUCCESS
    val report = testReport(module)
    report.tests shouldBeGreaterThan 0
    report.failures shouldBe 0
    report.errors shouldBe 0
}

/** Native 書き直し版が「元と同じ」テストを通す: KSP2 で走るテストと同じ件数が、Native でも全部通る。 */
private fun RealWorldSample.shouldPassSameTestsAs(
    engine: SampleEngine,
    module: String = "",
) {
    shouldPassRuntimeTests(SampleEngine.Ksp2, module)
    val expected = testReport(module)
    shouldPassRuntimeTests(engine, module)
    val actual = testReport(module)
    actual.tests shouldBe expected.tests
    actual.skipped shouldBe expected.skipped
}

/** 上流のテストスイートを KSP2 と Bridge で流し、Bridge でも同じ件数が全部通る（テストが黙って減っていない）。 */
private fun upstreamSuiteShouldPassOnBridge(
    id: String,
    vararg tasks: String,
) {
    val dir = UpstreamSuite.checkout(id)
    UpstreamSuite.run(dir, SampleEngine.Ksp2, "clean", *tasks)
    val expected = TestReport.read(dir)
    expected.passedSomething shouldBe true

    UpstreamSuite.run(dir, SampleEngine.Bridge, "clean", *tasks)
    val actual = TestReport.read(dir)
    actual.tests shouldBe expected.tests
    actual.failures shouldBe 0
    actual.errors shouldBe 0
}

class RealWorldE2ETest : E2ESpec({
    // ---- cream.kt ----
    case("RW-CRM-B1", "cream.kt（Bridge）: サンプルプロジェクトがビルドでき、生成コードを使う実行時テストが通る") {
        val sample = RealWorldSample.open("CRM", "jvm")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        sample.generated(SampleEngine.Bridge).keys.shouldNotBeEmpty()
    }

    case("RW-CRM-B3", "cream.kt（Bridge）: cream.kt 自身のテストスイートが Bridge 上で通る", pending = PendingCase(Milestone.M3)) {
        upstreamSuiteShouldPassOnBridge("CRM", ":test:allTests")
    }

    case("RW-CRM-N1", "cream.kt（Native）: 書き直し版が元と同じ実行時テストを通る") {
        RealWorldSample.open("CRM", "jvm").shouldPassSameTestsAs(SampleEngine.Native)
    }

    case(
        "RW-CRM-N2",
        "cream.kt（Native）: KMP の commonMain とプラットフォームで生成でき、共通部分は 1 回だけ作られる",
        pending = PendingCase(Milestone.M2),
    ) {
        // kmp サンプルは commonMain に @CopyTo などを置き、jvm と js でテストする（Apple は mac ランナーが要るので入れない）
        val sample = RealWorldSample.open("CRM", "kmp")
        sample.shouldPassRuntimeTests(SampleEngine.Native, testTask = "allTests")
        val common = sample.generated(SampleEngine.Native, "commonMain")
        common.keys.shouldNotBeEmpty()
        // 共通部分は commonMain の実行単位でだけ作られ、プラットフォームの実行単位には同じファイルが無い
        for (platform in listOf("jvmMain", "jsMain")) {
            val names = sample.generated(SampleEngine.Native, platform).keys
            common.keys.filter { it in names } shouldBe emptyList()
        }
    }

    // ---- Moshi ----
    case("RW-MSH-B1", "Moshi（Bridge）: サンプルがビルドでき、JSON の往復テストが通る") {
        val sample = RealWorldSample.open("MSH", "jvm")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        // Moshi は @JsonClass(generateAdapter = true) ごとに <Class>JsonAdapter を作る
        sample.generated(SampleEngine.Bridge).keys.any { it.endsWith("JsonAdapter.kt") } shouldBe true
    }

    case("RW-MSH-B3", "Moshi（Bridge）: Moshi の KSP 向けテストスイートが Bridge 上で通る", pending = PendingCase(Milestone.M3)) {
        upstreamSuiteShouldPassOnBridge("MSH", ":moshi-kotlin-tests:test", ":moshi-kotlin-codegen:test")
    }

    case("RW-MSH-N1", "Moshi（Native）: 書き直し版が元と同じ JSON 往復テストを通る", pending = PendingCase(Milestone.M2)) {
        RealWorldSample.open("MSH", "jvm").shouldPassSameTestsAs(SampleEngine.Native)
    }

    // ---- kotlin-inject ----
    case("RW-KIN-B1", "kotlin-inject（Bridge）: JVM のサンプルがビルドでき、DI の実行時テストが通る") {
        val sample = RealWorldSample.open("KIN", "jvm")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        // @Component abstract class AppComponent から InjectAppComponent が作られる
        sample.generated(SampleEngine.Bridge).keys.any { it.substringAfterLast('/').startsWith("Inject") } shouldBe true
    }

    case("RW-KIN-B3", "kotlin-inject（Bridge）: kotlin-inject 自身のテストスイートが Bridge 上で通る", pending = PendingCase(Milestone.M3)) {
        upstreamSuiteShouldPassOnBridge("KIN", ":integration-tests:kotlin-inject-test:jvmTest")
    }

    case("RW-KIN-N1", "kotlin-inject（Native）: 書き直し版が元と同じ DI の実行時テストを通る", pending = PendingCase(Milestone.Optional)) {
        RealWorldSample.open("KIN", "jvm").shouldPassSameTestsAs(SampleEngine.Native)
    }

    case("RW-KIN-N2", "kotlin-inject（Native）: KMP でモジュールをまたぐ DI グラフを生成できる", pending = PendingCase(Milestone.Optional)) {
        // kmp-multi サンプル: :core（commonMain に @Inject の型）と :app（@Component が :core の型を使う）
        val sample = RealWorldSample.open("KIN", "kmp-multi")
        sample.shouldPassRuntimeTests(SampleEngine.Native, module = "app", testTask = "jvmTest")
        sample.generated(SampleEngine.Native, "commonMain", module = "app").keys.shouldNotBeEmpty()
    }

    // ---- Koin Annotations ----
    case("RW-KOI-B1", "Koin Annotations（Bridge）: サンプルがビルドでき、DI の実行時テストが通る") {
        val sample = RealWorldSample.open("KOI", "jvm")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        // @Module class AppModule から AppModuleGencode（module 定義）が作られる
        sample.generated(SampleEngine.Bridge).keys.shouldNotBeEmpty()
    }

    case(
        "RW-KOI-B3",
        "Koin Annotations（Bridge）: Koin Annotations 自身のテストスイートが Bridge 上で通る",
        pending = PendingCase(Milestone.M3),
    ) {
        upstreamSuiteShouldPassOnBridge("KOI", "jvmTest")
    }

    case("RW-KOI-N1", "Koin Annotations（Native）: 書き直し版が元と同じ DI の実行時テストを通る", pending = PendingCase(Milestone.M2)) {
        RealWorldSample.open("KOI", "jvm").shouldPassSameTestsAs(SampleEngine.Native)
    }

    case("RW-KOI-N2", "Koin Annotations（Native）: KMP でモジュールをまたぐ定義を生成できる", pending = PendingCase(Milestone.M2)) {
        // kmp-multi サンプル: :data の @Module を :app の @Module(includes = ...) が取り込み、:app のテストで両方の定義を解決する
        val sample = RealWorldSample.open("KOI", "kmp-multi")
        sample.shouldPassRuntimeTests(SampleEngine.Native, module = "app", testTask = "jvmTest")
        sample.generated(SampleEngine.Native, "commonMain", module = "data").keys.shouldNotBeEmpty()
        sample.generated(SampleEngine.Native, "commonMain", module = "app").keys.shouldNotBeEmpty()
    }

    // ---- 複数適用 ----
    case(
        "RW-CMB-01",
        "複数適用: Koin + cream.kt を同じモジュールに適用し、Bridge 版で両方の生成と実行時テストが通る",
    ) {
        val sample = RealWorldSample.open("CMB", "koin-cream")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        val generated = sample.generated(SampleEngine.Bridge)
        // 両方の Processor の生成物がそろう（片方だけ動いて黙って通るのを防ぐ）。生成物の先頭行で出所を見分ける
        generated.keys.any { it.contains("/org/koin/") || generated.getValue(it).contains("org.koin.core") } shouldBe true
        generated.values.any { it.contains("me.tbsten.cream") || it.contains("copyTo") || it.contains("copyFrom") } shouldBe true
    }

    case(
        "RW-CMB-02",
        "複数適用: Koin + cream.kt の Native 版どうし、および Native と Bridge を混ぜた構成で同じ結果になる",
        pending = PendingCase(Milestone.M2),
    ) {
        val sample = RealWorldSample.open("CMB", "koin-cream")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        val bridge = sample.testReport()
        sample.shouldPassRuntimeTests(SampleEngine.Native)
        sample.testReport().tests shouldBe bridge.tests
        // 混ぜた構成: Koin は Native、cream.kt は Bridge（-Pacg.fixture.mix=KOI:native,CRM:bridge で切り替える。ADR-037）
        val mixed = sample.runner(SampleEngine.Native, "clean", "test", "-Pacg.fixture.mix=KOI:native,CRM:bridge").build()
        mixed.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
        sample.testReport().tests shouldBe bridge.tests
        sample.testReport().failures shouldBe 0
    }

    case(
        "RW-CMB-03",
        "複数適用: Hilt + Room + Moshi の Android サンプルが Bridge 版でビルドでき、実行時テストが通る",
        pending = PendingCase(Milestone.M3),
    ) {
        // F-AND と同じ構成のアプリ。Robolectric で DAO・JSON・注入を通す
        val sample = RealWorldSample.open("CMB", "hilt-room-moshi-android")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge, module = "app", testTask = "testDebugUnitTest")
    }

    // ---- Room ----
    case(
        "RW-ROM-B1",
        "Room（Bridge）: Android サンプルがビルドでき、DAO の instrumented ではない実行時テスト（Robolectric）が通る",
        pending = PendingCase(Milestone.M2),
    ) {
        val sample = RealWorldSample.open("ROM", "android")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge, module = "app", testTask = "testDebugUnitTest")
        // Room は @Database ごとに <Database>_Impl を作る
        sample
            .generated(
                SampleEngine.Bridge,
                "debug",
                module = "app",
            ).keys
            .any { it.endsWith("_Impl.kt") || it.endsWith("_Impl.java") } shouldBe
            true
    }

    case("RW-ROM-B3", "Room（Bridge）: KMP 版 Room のサンプルがビルドできる", pending = PendingCase(Milestone.M3)) {
        val sample = RealWorldSample.open("ROM", "kmp")
        val result = sample.build(SampleEngine.Bridge, "clean", "jvmTest")
        result.task(":jvmTest")?.outcome shouldBe TaskOutcome.SUCCESS
        result.output shouldNotContain "e: "
    }

    // ---- Dagger / Hilt ----
    case("RW-DAG-B1", "Dagger（Bridge）: JVM サンプルがビルドでき、DI の実行時テストが通る", pending = PendingCase(Milestone.M2)) {
        val sample = RealWorldSample.open("DAG", "jvm")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge)
        // @Component interface AppComponent から DaggerAppComponent が作られる
        sample.generated(SampleEngine.Bridge).keys.any { it.substringAfterLast('/').startsWith("Dagger") } shouldBe true
    }

    case(
        "RW-DAG-B2",
        "Hilt（Bridge）: Android サンプルがビルドでき、複数ラウンド・集約生成を含めて実行時テストが通る",
        pending = PendingCase(Milestone.M3),
    ) {
        val sample = RealWorldSample.open("DAG", "hilt-android")
        sample.shouldPassRuntimeTests(SampleEngine.Bridge, module = "app", testTask = "testDebugUnitTest")
        // 集約生成（@HiltAndroidApp の Hilt_<App> と、:lib の @InstallIn モジュールを集める aggregated deps）がそろう
        val generated = sample.generated(SampleEngine.Bridge, "debug", module = "app").keys
        generated.any { it.substringAfterLast('/').startsWith("Hilt_") } shouldBe true
        generated.any { "hilt_aggregated_deps" in it } shouldBe true
    }

    case("RW-DAG-N1", "Dagger / Hilt（Native）: 書き直し版が元と同じ DI の実行時テストを通る", pending = PendingCase(Milestone.Optional)) {
        RealWorldSample.open("DAG", "jvm").shouldPassSameTestsAs(SampleEngine.Native)
    }
})
