@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysiscodegen.enginetests.cch

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import me.tbsten.analysiscodegen.analysis.ModuleId
import me.tbsten.analysiscodegen.analysis.ReferenceAnalysisEnvironmentFactory
import me.tbsten.analysiscodegen.analysis.SourceChangeSet
import me.tbsten.analysiscodegen.analysis.SourceFileId
import me.tbsten.analysiscodegen.analysis.StandaloneAnalysisEnvironmentFactory
import me.tbsten.analysiscodegen.enginetests.support.EngineProject
import me.tbsten.analysiscodegen.enginetests.support.EngineSourceSet
import me.tbsten.analysiscodegen.enginetests.support.RecordingEnvironmentFactory
import me.tbsten.analysiscodegen.enginetests.support.engineWith
import me.tbsten.analysiscodegen.model.PlatformKind
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessor
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorEnvironment
import me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider
import me.tbsten.analysiscodegen.processor.AnalysisCodegenRoundContext
import me.tbsten.analysiscodegen.processor.Dependencies
import me.tbsten.analysiscodegen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysiscodegen.testfoundation.DiffSpec
import me.tbsten.analysiscodegen.testfoundation.IntegrationSpec
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.psi.KtClass

// 5.9 キャッシュ再利用とモジュール無効化（CCH / MOD）。環境は AnalysisEnvironmentFactory を包んで観察する（ADR-034）

/**
 * `@Gen` を付けたクラスごとに、ラウンド 1 で `<Name>Gen1`、ラウンド 2 で `<Name>Gen2` を作る（ラウンドを 3 回起こす）。
 * 生成物はそれぞれ前のラウンドの生成物を参照するので、差分更新が漏れると解決に失敗する。
 */
class ChainGenProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val classes = context.newFiles.flatMap { f -> f.declarations.filterIsInstance<KtClass>().map { f to it } }
                classes.forEach { (file, cls) ->
                    val name = cls.name ?: return@forEach
                    val pkg = file.packageFqName.asString()
                    when {
                        cls.annotationEntries.any { it.shortName?.asString() == "Gen" } ->
                            context.codeGenerator.generateKotlin(
                                pkg,
                                "${name}Gen1",
                                Dependencies.isolating(file),
                                "package $pkg\n\nclass ${name}Gen1(val origin: $name)",
                            )
                        name.endsWith("Gen1") ->
                            context.codeGenerator.generateKotlin(
                                pkg,
                                name.replace("Gen1", "Gen2"),
                                Dependencies.isolating(file),
                                "package $pkg\n\nclass ${name.replace("Gen1", "Gen2")}(val prev: $name)",
                            )
                    }
                }
            }
        }
}

/** ラウンドごとに、見えているライブラリモジュールの identity を記録する（CCH-02）。 */
class LibraryProbeProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                val libs =
                    analyze(context.sourceSet.module) {
                        context.sourceSet.module.directRegularDependencies
                            .filterIsInstance<KaLibraryModule>()
                    }
                libraryIdentities += libs.map { System.identityHashCode(it) }.sorted()
                if (context.round == 1) {
                    val file = context.allFiles.first()
                    context.codeGenerator.generateKotlin("probe", "Next", Dependencies.isolating(file), "package probe\n\nobject Next")
                }
            }
        }

    companion object {
        val libraryIdentities = mutableListOf<List<Int>>()
    }
}

private fun genClass(name: String) =
    "package com.example\n\nannotation class Gen\n\n@Gen class $name".let {
        if (name ==
            "A"
        ) {
            it
        } else {
            "package com.example\n\n@Gen class $name"
        }
    }

class CacheReuseIntegrationTest : IntegrationSpec({
    case("CCH-01", "ラウンド間で IntelliJ Project・Module Graph・Library Index を作り直さない（構築回数が 1 回）", pending = PendingCase(Milestone.M2)) {
        val project =
            EngineProject().apply {
                kotlin("com/example/A.kt", genClass("A"))
                nativeProcessor(ChainGenProvider::class)
            }
        val factory = RecordingEnvironmentFactory()
        val result = project.run(engineWith(factory))
        result.success shouldBe true
        result.rounds shouldBe 3
        factory.created.get() shouldBe 1
        project.generated().keys.map { it.substringAfterLast('/') } shouldContainAll listOf("AGen1.kt", "AGen2.kt")
    }

    case("CCH-02", "ラウンド間でライブラリ / バイナリのキャッシュを捨てない（KSP のコミット 958d32f 相当）", pending = PendingCase(Milestone.M2)) {
        LibraryProbeProvider.libraryIdentities.clear()
        val project =
            EngineProject().apply {
                kotlin("com/example/A.kt", "package com.example\n\nclass A")
                nativeProcessor(LibraryProbeProvider::class)
            }
        project.run().success shouldBe true
        // ラウンド 1 と 2 で、同じライブラリモジュールのインスタンスが見えている
        LibraryProbeProvider.libraryIdentities.size shouldBe 2
        LibraryProbeProvider.libraryIdentities[0].isNotEmpty() shouldBe true
        LibraryProbeProvider.libraryIdentities[1] shouldBe LibraryProbeProvider.libraryIdentities[0]
    }

    case("CCH-03", "生成ファイルが増えたとき、ソースインデックスを差分更新し全体を再構築しない", pending = PendingCase(Milestone.M2)) {
        val project =
            EngineProject().apply {
                kotlin("com/example/A.kt", genClass("A"))
                repeat(20) { kotlin("com/example/Plain$it.kt", "package com.example\n\nclass Plain$it") }
                nativeProcessor(ChainGenProvider::class)
            }
        val factory = RecordingEnvironmentFactory()
        project.run(engineWith(factory)).success shouldBe true
        factory.created.get() shouldBe 1
        // 差分反映はラウンドごとに生成物だけを足す（既存ソースの再読み込み・削除は無い）
        factory.invalidations.size shouldBe 2
        factory.invalidations.forEach { (change, result) ->
            change.added.keys
                .map { it.path.substringAfterLast('/') }
                .all { "Gen" in it } shouldBe true
            change.modified shouldBe emptyMap()
            change.removed shouldBe emptySet()
            result.modifiedFiles shouldBe emptySet()
            result.removedFiles shouldBe emptySet()
        }
    }

    case("MOD-01", "KMP で iosMain に生成したとき、jvmMain / androidMain のセッションは保たれ、iosArm64Main 等は無効化される", pending = PendingCase(Milestone.M2)) {
        val kmp =
            EngineProject(
                listOf(
                    EngineSourceSet("commonMain", PlatformKind.Common),
                    EngineSourceSet("jvmMain", PlatformKind.Jvm, listOf("commonMain")),
                    EngineSourceSet("androidMain", PlatformKind.Android, listOf("commonMain")),
                    EngineSourceSet("iosMain", PlatformKind.Native, listOf("commonMain")),
                    EngineSourceSet("iosArm64Main", PlatformKind.Native, listOf("iosMain")),
                    EngineSourceSet("iosSimulatorArm64Main", PlatformKind.Native, listOf("iosMain")),
                ),
            )
        kmp.kotlin("com/example/Ios.kt", genClass("A"), sourceSet = "iosMain")
        kmp.nativeProcessor(ChainGenProvider::class)
        val env = StandaloneAnalysisEnvironmentFactory().create(kmp.model(), "iosMain")
        val result =
            env.applyChanges(
                SourceChangeSet(
                    ModuleId("iosMain"),
                    added =
                        mapOf(
                            SourceFileId("build/generated/analysisCodeGen/iosMain/kotlin/com/example/AGen1.kt") to
                                "package com.example\n\nclass AGen1(val origin: A)",
                        ),
                    modified = emptyMap(),
                    removed = emptySet(),
                ),
            )
        val affected = result.affectedModules.map { it.value }.toSet()
        affected shouldContainAll listOf("iosMain", "iosArm64Main", "iosSimulatorArm64Main")
        affected shouldNotContain "jvmMain"
        affected shouldNotContain "androidMain"
        affected shouldNotContain "commonMain"
    }

    case("MOD-02", "無効化のあと、影響を受けたモジュールで新しい宣言が解決できる（無効化漏れが無い）", pending = PendingCase(Milestone.M2)) {
        // jvmMain に生成した宣言を、jvmMain のソースと次のラウンドの生成物から参照できる（ChainGen の Gen2 は Gen1 を参照する）
        val kmp =
            EngineProject(
                listOf(
                    EngineSourceSet("commonMain", PlatformKind.Common),
                    EngineSourceSet("jvmMain", PlatformKind.Jvm, listOf("commonMain")),
                ),
            )
        kmp.kotlin("com/example/A.kt", genClass("A"), sourceSet = "commonMain")
        kmp.kotlin("com/example/UsesGen.kt", "package com.example\n\nfun use(x: AGen1): A = x.origin", sourceSet = "commonMain")
        kmp.nativeProcessor(ChainGenProvider::class)
        val result = kmp.run(sourceSet = "commonMain")
        result.success shouldBe true
        result.diagnostics.filter { it.severity == me.tbsten.analysiscodegen.engine.Diagnostic.Severity.Error } shouldBe emptyList()
        kmp.generated("commonMain").keys.map { it.substringAfterLast('/') } shouldContain "AGen2.kt"
    }

    case("MOD-03", "friend / regular / expect-actual の関係を通じた影響も無効化される", pending = PendingCase(Milestone.M2)) {
        val kmp =
            EngineProject(
                listOf(
                    EngineSourceSet("commonMain", PlatformKind.Common),
                    EngineSourceSet("jvmMain", PlatformKind.Jvm, listOf("commonMain")),
                    EngineSourceSet("commonTest", PlatformKind.Common, listOf("commonMain")),
                ),
            )
        kmp.kotlin("com/example/Expect.kt", "package com.example\n\nexpect fun platform(): String", sourceSet = "commonMain")
        kmp.kotlin("com/example/Actual.kt", "package com.example\n\nactual fun platform(): String = \"jvm\"", sourceSet = "jvmMain")
        val env = StandaloneAnalysisEnvironmentFactory().create(kmp.model(), "commonMain")
        val result =
            env.applyChanges(
                SourceChangeSet(
                    ModuleId("commonMain"),
                    added =
                        mapOf(
                            SourceFileId("build/generated/analysisCodeGen/commonMain/kotlin/com/example/New.kt") to
                                "package com.example\n\nexpect class New",
                        ),
                    modified = emptyMap(),
                    removed = emptySet(),
                ),
            )
        val affected = result.affectedModules.map { it.value }.toSet()
        // regular（dependsOn）: jvmMain、friend（テスト → main）: commonTest、expect-actual: actual を持つ jvmMain
        affected shouldContainAll listOf("commonMain", "jvmMain", "commonTest")
        env.moduleGraph.dependents(ModuleId("commonMain")).map { it.value } shouldContainAll listOf("jvmMain", "commonTest")
    }
})

class CacheReuseDiffTest : DiffSpec({
    case("CCH-05", "差分無効化した環境と、毎ラウンド作り直す Reference Engine で、ランダムなシナリオの解析・生成結果が一致する", pending = PendingCase(Milestone.M2)) {
        // シナリオ: @Gen 付き / 無しのクラスを n 個置き、ラウンドを連鎖させる。ランダムに選んだクラスが前の生成物を参照する
        checkAll(PropTestConfig(iterations = 30), Arb.list(Arb.int(0..3), 1..8)) { kinds ->
            fun build(factoryEngine: me.tbsten.analysiscodegen.engine.AnalysisCodegenEngine): Pair<Boolean, Map<String, String>> {
                val p = EngineProject()
                p.kotlin("com/example/Gen.kt", "package com.example\n\nannotation class Gen")
                kinds.forEachIndexed { i, k ->
                    val body =
                        when (k) {
                            0 -> "class C$i"
                            1 -> "@Gen class C$i"
                            2 -> "@Gen class C$i { fun use(x: C${i}Gen1?) = x }"
                            else -> "class C$i { fun use(x: C${maxOf(0, i - 1)}Gen2?) = x }"
                        }
                    p.kotlin("com/example/C$i.kt", "package com.example\n\n$body")
                }
                p.nativeProcessor(ChainGenProvider::class)
                val r = p.run(factoryEngine)
                return r.success to p.generated()
            }
            val incremental = build(engineWith(StandaloneAnalysisEnvironmentFactory()))
            val reference = build(engineWith(ReferenceAnalysisEnvironmentFactory()))
            incremental shouldBe reference
        }
    }
})
