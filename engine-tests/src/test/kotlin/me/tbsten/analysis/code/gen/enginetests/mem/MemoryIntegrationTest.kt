@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.mem

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import me.tbsten.analysis.code.gen.analysis.StandaloneAnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.enginetests.support.NATIVE_SERVICE
import me.tbsten.analysis.code.gen.enginetests.ver.compileToClasses
import me.tbsten.analysis.code.gen.enginetests.ver.processorJar
import me.tbsten.analysis.code.gen.model.AnalysisConfig
import me.tbsten.analysis.code.gen.model.CompilationModel
import me.tbsten.analysis.code.gen.model.CompilerOptionsModel
import me.tbsten.analysis.code.gen.model.DeferredSeverityModel
import me.tbsten.analysis.code.gen.model.ExecutionUnitModel
import me.tbsten.analysis.code.gen.model.OutputDirsModel
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.model.ProcessingScopeModel
import me.tbsten.analysis.code.gen.model.ProjectModel
import me.tbsten.analysis.code.gen.model.SourceSetModel
import me.tbsten.analysis.code.gen.model.UnitKind
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import me.tbsten.analysis.code.gen.testfoundation.Milestone
import me.tbsten.analysis.code.gen.testfoundation.PendingCase
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import java.io.File
import java.lang.ref.WeakReference
import java.nio.file.Files

// 5.24 メモリ（MEM）のうち解析環境と ClassLoader の解放（MEM-01・04）。AA・IntelliJ の型に触れるので :engine-tests に置く（ADR-034・037）。
// MEM-02・03（Gradle の daemon の上でのヒープ）は e2e-tests の e2e.mem

/**
 * 弱参照が GC で消えるまで待つ。消えなければ false。
 * 大きめの配列を作って GC を促す（System.gc() だけでは回収されないことがあるため）。
 */
private fun WeakReference<*>.awaitCleared(attempts: Int = 30): Boolean {
    repeat(attempts) {
        if (get() == null) return true
        System.gc()
        @Suppress("UNUSED_VARIABLE")
        val pressure = ByteArray(16 * 1024 * 1024)
        Thread.sleep(100)
    }
    return get() == null
}

/** JVM の 1 モジュール・1 ファイルのモデル（Compilation 単位 `main`）。 */
private fun singleFileModel(): ProjectModel {
    val root = Files.createTempDirectory("acg-mem").toFile()
    val src = root.resolve("src/main/kotlin").apply { mkdirs() }
    src.resolve("A.kt").writeText("package a\n\nclass A {\n    fun f(): Int = 1\n}\n")
    val stdlib =
        File(
            KotlinVersion::class.java.protectionDomain.codeSource.location
                .toURI(),
        )
    return ProjectModel(
        schemaVersion = 1,
        rootDir = root.absolutePath,
        sourceSets =
            listOf(
                SourceSetModel(
                    name = "main",
                    platform = PlatformKind.Jvm,
                    kotlinSourceRoots = listOf(src.absolutePath),
                    javaSourceRoots = emptyList(),
                    dependsOn = emptyList(),
                    upstreamGeneratedRoots = emptyList(),
                ),
            ),
        compilations =
            listOf(
                CompilationModel(
                    name = "main",
                    targetName = "",
                    compilationName = "main",
                    platform = PlatformKind.Jvm,
                    defaultSourceSet = "main",
                    sourceSets = listOf("main"),
                    classpath = listOf(stdlib.absolutePath),
                    friendPaths = emptyList(),
                    jdkHome = System.getProperty("java.home"),
                    compilerOptions = CompilerOptionsModel(null, null, emptyList(), emptyList(), "17"),
                ),
            ),
        units =
            listOf(
                ExecutionUnitModel(
                    name = "main",
                    kind = UnitKind.Compilation,
                    target = "",
                    analysisCompilation = "main",
                    processingScope = ProcessingScopeModel.Own,
                    processors = emptyList(),
                    options = emptyMap(),
                    outputDirs =
                        OutputDirsModel(
                            kotlin = root.resolve("out/kotlin").absolutePath,
                            java = root.resolve("out/java").absolutePath,
                            resources = root.resolve("out/resources").absolutePath,
                        ),
                    stagingDir = root.resolve("staging").absolutePath,
                    cacheDir = root.resolve("cache").absolutePath,
                ),
            ),
        // AA は Kotlin と同じ版で出る（ADR-011）。テストの実行時の Kotlin を使う
        analysis = AnalysisConfig(KotlinVersion.CURRENT.toString(), 100, DeferredSeverityModel.Error, null),
    )
}

/** 解析環境を作って閉じ、Project への弱参照を返す。強い参照をこの関数の外に残さないため、関数に分けている。 */
private fun openAndCloseEnvironment(model: ProjectModel): WeakReference<Project> {
    val environment = StandaloneAnalysisEnvironmentFactory().create(model, "main")
    val project = environment.project
    var disposedWithEnvironment = false
    // Project に紐づく Disposable が環境を閉じたときに一緒に破棄されるか（登録が残らないか）を見る
    Disposer.register(project) { disposedWithEnvironment = true }
    environment.close()
    withClue("環境を閉じたら Project は dispose 済み") { Disposer.isDisposed(project) shouldBe true }
    withClue("Project に登録した Disposable も破棄される") { disposedWithEnvironment shouldBe true }
    return WeakReference(project)
}

private const val MEM04_PROVIDER = "acg.mem04.LoaderProbeProvider"

/** 自分を読んだ ClassLoader を、system property に弱参照で残す Processor（テストの classpath には無い）。 */
private val LOADER_PROBE =
    mapOf(
        "acg/mem04/LoaderProbeProvider.kt" to
            """
            package acg.mem04

            import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessor
            import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorEnvironment
            import me.tbsten.analysis.code.gen.processor.AnalysisCodegenProcessorProvider
            import me.tbsten.analysis.code.gen.processor.AnalysisCodegenRoundContext

            class LoaderProbeProvider : AnalysisCodegenProcessorProvider {
                override fun create(environment: AnalysisCodegenProcessorEnvironment): AnalysisCodegenProcessor {
                    val key = "acg.mem04.loader." + environment.options.getValue("run")
                    System.getProperties()[key] = java.lang.ref.WeakReference(LoaderProbeProvider::class.java.classLoader)
                    return object : AnalysisCodegenProcessor {
                        override fun process(context: AnalysisCodegenRoundContext) {}
                    }
                }
            }
            """,
    )

class MemoryIntegrationTest : IntegrationSpec({
    case(
        "MEM-01",
        "解析環境を閉じたあと、IntelliJ Project と Disposer の登録が残らない（GC 後に回収されている）",
    ) {
        val model = singleFileModel()
        // 1 回目は AA のアプリケーション環境（プロセスで 1 つ、ADR-010）を作るので、2 回目以降の Project を見る
        openAndCloseEnvironment(model)
        val projects = (1..3).map { openAndCloseEnvironment(model) }
        projects.forEachIndexed { i, ref ->
            // Disposer の木に登録が残っていれば Project は強く参照され続けるので、回収されたことが「登録が残らない」ことの確かめになる
            withClue("${i + 2} 回目の Project が GC で回収されない（Disposer などに参照が残っている）") { ref.awaitCleared() shouldBe true }
        }
    }

    case("MEM-04", "ビルド終了後に Processor の ClassLoader が解放される", pending = PendingCase(Milestone.M2)) {
        val jar = processorJar(compileToClasses(LOADER_PROBE), mapOf(NATIVE_SERVICE to listOf(MEM04_PROVIDER)))
        val refs =
            (1..3).map { run ->
                val result =
                    runCodegenTest(TestEngine.AnalysisCodegen) {
                        kotlin("com/example/A.kt", "package com.example\n\nclass A")
                        processorJar(jar)
                        options["run"] = run.toString()
                    }
                withClue(result.messages) { result.isSuccess shouldBe true }
                val key = "acg.mem04.loader.$run"

                @Suppress("UNCHECKED_CAST")
                val ref = System.getProperties().remove(key) as WeakReference<ClassLoader>?
                withClue("Processor が create されていない") { (ref != null) shouldBe true }
                // テストの classpath ではなく、エンジンが Processor のために作った ClassLoader から読まれている
                ref!!.get()?.let { it shouldNotBeSameInstanceAs MemoryIntegrationTest::class.java.classLoader }
                ref
            }
        refs.forEachIndexed { i, ref ->
            withClue("${i + 1} 回目のビルドの Processor の ClassLoader が解放されない") { ref.awaitCleared() shouldBe true }
        }
        refs.last().get().shouldBeNull()
    }
})
