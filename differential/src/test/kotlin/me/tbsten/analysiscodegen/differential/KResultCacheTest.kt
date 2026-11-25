package me.tbsten.analysiscodegen.differential

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import me.tbsten.analysiscodegen.testprocessors.ksp.MapperKspProvider
import java.io.File
import java.nio.file.Files

private val versions = EngineVersions(kotlin = "2.4.21", ksp = "2.3.14")

private fun baseInput() =
    DiffInput(
        name = "cache",
        sources =
            listOf(
                TpInputs.annotations,
                DiffSource("com/example/A.kt", "package com.example\n\n@me.tbsten.analysiscodegen.tp.Mapped\nclass A(val x: Int)\n"),
            ),
        processors = listOf(TpInputs.mapper),
        options = mapOf("tp.processors" to "Mapper", "z" to "1"),
    )

private val sampleOutput =
    RunOutput(
        success = false,
        files = sortedMapOf("kotlin/A.kt" to "class A\n", "resources/META-INF/x" to "日本語\r\n"),
        diagnostics =
            listOf(
                DiagnosticRecord(DiagnosticRecord.Severity.Error, "e", "A.kt:1", fromProcessor = true),
                DiagnosticRecord(DiagnosticRecord.Severity.Warning, "w\nmulti", null),
            ),
        rounds = listOf(RoundRecord(1, listOf("kotlin/A.kt")), RoundRecord(2, emptyList())),
        roots = listOf("/tmp/x"),
    )

class KResultCacheTest : UnitSpec({
    case("XD-17", "K の結果のキャッシュ（入力のハッシュ → 出力）が、入力・Processor・オプション・KSP のバージョンが変わったら効かなくなる") {
        val base = baseInput()
        val key = KCacheKey.of(base, versions)

        // 同じ入力なら同じ鍵（ソースとオプションの並びは効かない）
        KCacheKey.of(baseInput(), versions) shouldBe key
        KCacheKey.of(
            base.copy(
                sources = base.sources.reversed(),
                options =
                    base.options.entries.reversed().associate {
                        it.toPair()
                    },
            ),
            versions,
        ) shouldBe
            key
        // 名前と期待（Valid / Invalid）は出力に効かないので鍵に入れない
        KCacheKey.of(base.copy(name = "other", expectation = Expectation.Invalid), versions) shouldBe key

        // 変わったら効かなくなるもの
        val changed =
            mapOf(
                "ソースの中身" to
                    KCacheKey.of(
                        base.copy(
                            sources =
                                base.sources.map {
                                    if (it.path.endsWith("A.kt")) {
                                        it.copy(
                                            content =
                                                it.content + "// x\n",
                                        )
                                    } else {
                                        it
                                    }
                                },
                        ),
                        versions,
                    ),
                "ソースのパス" to
                    KCacheKey.of(
                        base.copy(sources = base.sources.map { if (it.path.endsWith("A.kt")) it.copy(path = "com/example/B.kt") else it }),
                        versions,
                    ),
                "ソースの追加" to
                    KCacheKey.of(base.copy(sources = base.sources + DiffSource("com/example/C.kt", "package com.example\n")), versions),
                "SourceSet" to KCacheKey.of(base.copy(sources = base.sources.map { it.copy(sourceSet = "jvmMain") }), versions),
                "Processor の追加" to KCacheKey.of(base.copy(processors = base.processors + TpInputs.registry), versions),
                "Processor の順番" to KCacheKey.of(base.copy(processors = listOf(TpInputs.registry, TpInputs.mapper)), versions),
                "Processor の版" to
                    KCacheKey.of(
                        base.copy(processors = listOf(DiffProcessor("TP-Mapper", "test2", ksp = { MapperKspProvider() }))),
                        versions,
                    ),
                "Processor の id" to
                    KCacheKey.of(
                        base.copy(processors = listOf(DiffProcessor("TP-Mapper2", "test", ksp = { MapperKspProvider() }))),
                        versions,
                    ),
                "オプションの値" to KCacheKey.of(base.copy(options = base.options + ("z" to "2")), versions),
                "オプションの追加" to KCacheKey.of(base.copy(options = base.options + ("y" to "1")), versions),
                "オプションの削除" to KCacheKey.of(base.copy(options = base.options - "z"), versions),
                "KSP の版" to KCacheKey.of(base, versions.copy(ksp = "2.3.15")),
                "Kotlin の版" to KCacheKey.of(base, versions.copy(kotlin = "2.4.30")),
                "language version" to KCacheKey.of(base.copy(languageVersion = "2.4"), versions),
            )
        changed.forEach { (what, k) -> withClue(what) { k shouldNotBe key } }
        changed.values.toSet().size shouldBe changed.size

        // Processor の実装（クラスの置き場所の中身）が変われば、版が同じでも効かなくなる
        val dir = Files.createTempDirectory("acg-fp").toFile()
        val jar = dir.resolve("p.jar").apply { writeText("v1") }
        val cp1 = KCacheKey.of(base.copy(classpath = listOf(jar)), versions)
        jar.writeText("v2-changed")
        jar.setLastModified(jar.lastModified() + 2_000)
        KCacheKey.of(base.copy(classpath = listOf(jar)), versions) shouldNotBe cp1

        // Kotlin の版ごとにディレクトリが分かれ、別の版の結果は読まれない
        val store = FileKResultStore(Files.createTempDirectory("acg-k-cache").toFile())
        store.put(key, sampleOutput)
        store.get(key) shouldBe sampleOutput
        store.get(KCacheKey.of(base, versions.copy(kotlin = "2.4.30"))).shouldBeNull()
        store.get(KCacheKey.of(base, versions.copy(ksp = "2.3.15"))).shouldBeNull()
        store.directory.resolve("2.4.21").isDirectory shouldBe true

        // ランナーは当たれば動かさず、外れれば動かす
        var executions = 0
        val runner =
            KspReferenceRunner(InMemoryKResultStore(), versions) {
                executions++
                sampleOutput
            }
        runner
            .run(base)
            .shouldBeInstanceOf<EngineRun.Completed>()
            .metrics.cacheHit shouldBe false
        runner
            .run(baseInput())
            .shouldBeInstanceOf<EngineRun.Completed>()
            .metrics.cacheHit shouldBe true
        executions shouldBe 1
        runner
            .run(base.copy(options = base.options + ("z" to "2")))
            .shouldBeInstanceOf<EngineRun.Completed>()
            .metrics.cacheHit shouldBe
            false
        executions shouldBe 2
        KspReferenceRunner(InMemoryKResultStore(), versions.copy(ksp = "2.3.15")) {
            executions++
            sampleOutput
        }.run(base)
        executions shouldBe 3
    }

    "キャッシュは読み書きで中身が変わらず、壊れたファイルは無いものとして扱う" {
        val dir = Files.createTempDirectory("acg-k-cache").toFile()
        val store = FileKResultStore(dir)
        val key = KCacheKey.of(baseInput(), versions)
        store.put(key, sampleOutput.copy(rounds = null))
        store.get(key) shouldBe sampleOutput.copy(rounds = null)
        dir.walkTopDown().filter(File::isFile).forEach { it.writeText("broken") }
        store.get(key).shouldBeNull()
    }

    "実際の K の結果はキャッシュから読んでも同じ" {
        val store = InMemoryKResultStore()
        val runner = KspReferenceRunner(store)
        val first = runner.run(baseInput()).shouldBeInstanceOf<EngineRun.Completed>()
        val second = runner.run(baseInput()).shouldBeInstanceOf<EngineRun.Completed>()
        second.metrics.cacheHit shouldBe true
        second.output shouldBe first.output
        first.output.files.keys shouldBe setOf("kotlin/com/example/AMapper.kt")
    }
})
