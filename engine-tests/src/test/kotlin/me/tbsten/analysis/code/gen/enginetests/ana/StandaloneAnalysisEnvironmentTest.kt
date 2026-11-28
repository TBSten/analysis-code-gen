@file:OptIn(InternalAnalysisCodegenApi::class)

package me.tbsten.analysis.code.gen.enginetests.ana

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironment
import me.tbsten.analysis.code.gen.analysis.AnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.analysis.ModuleId
import me.tbsten.analysis.code.gen.analysis.ReferenceAnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.analysis.SourceChangeSet
import me.tbsten.analysis.code.gen.analysis.SourceFileId
import me.tbsten.analysis.code.gen.analysis.StandaloneAnalysisEnvironmentFactory
import me.tbsten.analysis.code.gen.enginetests.support.EngineProject
import me.tbsten.analysis.code.gen.enginetests.support.EngineSourceSet
import me.tbsten.analysis.code.gen.model.PlatformKind
import me.tbsten.analysis.code.gen.processor.InternalAnalysisCodegenApi
import me.tbsten.analysis.code.gen.testfoundation.IntegrationSpec
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File

// ID の無い補助テスト（ACG-61、ADR-060）。エンジン（ACG-62）を通さずに解析環境だけを動かし、
// ANA・KMP のケースが前提にする環境の性質（ライブラリ・Java・Module Graph・ラウンド間の差分反映）を確かめる

private fun cid(fqn: String) = ClassId(FqName(fqn.substringBeforeLast('.')), Name.identifier(fqn.substringAfterLast('.')))

private val MAIN = ModuleId("main")

/** テストの classpath にある JAR のうち、名前に [part] を含むもの。 */
private fun testJar(part: String): String =
    System
        .getProperty("java.class.path")
        .split(File.pathSeparator)
        .first { part in File(it).name }

private fun <R> EngineProject.withEnvironment(
    unit: String = unitName(),
    factory: AnalysisEnvironmentFactory = StandaloneAnalysisEnvironmentFactory(),
    extraClasspath: List<String> = emptyList(),
    block: (AnalysisEnvironment) -> R,
): R {
    val model = model().let { m -> m.copy(compilations = m.compilations.map { it.copy(classpath = it.classpath + extraClasspath) }) }
    return factory.create(model, unit).use(block)
}

/** [className] のスーパークラスを解決した結果（解決できなければ null）。 */
private fun AnalysisEnvironment.superclassOf(
    module: ModuleId,
    className: String,
): String? =
    analyze(sourceModule(module)) {
        val cls = findClass(cid(className)) as KaClassSymbol
        cls.superTypes
            .filterIsInstance<KaClassType>()
            .map { it.classId.asFqNameString() }
            .firstOrNull { it != "kotlin.Any" }
    }

class StandaloneAnalysisEnvironmentTest : IntegrationSpec({
    "JVM の 1 モジュールで、ソース・ライブラリ・JDK の宣言を解決できる" {
        val project = EngineProject()
        project.kotlin("a/A.kt", "package a\nclass A(val names: List<String>)")
        val seen =
            project.withEnvironment(extraClasspath = listOf(testJar("kotest-assertions-shared"))) { env ->
                env.kotlinFiles(MAIN).map { it.name } shouldBe listOf("A.kt")
                analyze(env.sourceModule(MAIN)) {
                    listOf(
                        findClass(cid("a.A"))?.origin,
                        findClass(cid("io.kotest.matchers.Matcher"))?.origin,
                        findClass(cid("java.util.concurrent.ConcurrentHashMap"))?.origin,
                    )
                }
            }
        seen shouldBe listOf(KaSymbolOrigin.SOURCE, KaSymbolOrigin.LIBRARY, KaSymbolOrigin.JAVA_LIBRARY)
    }

    "Java のソースを javaFiles で返し、KaSymbol として解決できる" {
        val project = EngineProject()
        project.kotlin("a/A.kt", "package a\nclass A : j.Base()")
        project.java("j/Base.java", "package j;\npublic class Base { public String name() { return \"\"; } }")
        project.withEnvironment { env ->
            env.javaFiles(MAIN).map { it.name } shouldBe listOf("Base.java")
            env.superclassOf(MAIN, "a.A") shouldBe "j.Base"
            analyze(env.sourceModule(MAIN)) { findClass(cid("j.Base"))?.origin } shouldBe KaSymbolOrigin.JAVA_SOURCE
        }
    }

    "KMP の commonMain と jvmMain を dependsOn でつなぎ、jvmMain から expect / actual と共通の宣言を解決できる" {
        val project =
            EngineProject(
                sourceSets =
                    listOf(
                        EngineSourceSet("commonMain", PlatformKind.Common),
                        EngineSourceSet("jvmMain", PlatformKind.Jvm, dependsOn = listOf("commonMain")),
                    ),
            )
        project.kotlin("c/Common.kt", "package c\nopen class Common\nexpect fun platform(): String", sourceSet = "commonMain")
        project.kotlin("j/Jvm.kt", "package j\nclass Jvm : c.Common()\nactual fun c.platformName() = \"\"", sourceSet = "jvmMain")
        project.kotlin("c/Actual.kt", "package c\nactual fun platform(): String = \"jvm\"", sourceSet = "jvmMain")
        project.withEnvironment(unit = project.unitName("jvmMain")) { env ->
            env.moduleGraph.modules shouldBe setOf(ModuleId("commonMain"), ModuleId("jvmMain"))
            env.moduleGraph.dependents(ModuleId("commonMain")) shouldBe setOf(ModuleId("jvmMain"))
            env.kotlinFiles(ModuleId("commonMain")).map { it.name } shouldBe listOf("Common.kt")
            env.kotlinFiles(ModuleId("jvmMain")).map { it.name } shouldBe listOf("Actual.kt", "Jvm.kt")
            env.superclassOf(ModuleId("jvmMain"), "j.Jvm") shouldBe "c.Common"
            analyze(env.sourceModule(ModuleId("jvmMain"))) {
                findTopLevelCallables(FqName("c"), Name.identifier("platform"))
                    .filter { it.isActual }
                    .flatMap { it.getExpectsForActual() }
                    .map { it.psi!!.containingFile.name }
                    .toList()
            } shouldBe listOf("Common.kt")
        }
    }

    "ラウンドの生成物を足すと、既存の PSI を保ったまま、未解決だった上位型が解決される" {
        val project = EngineProject()
        project.kotlin("a/User.kt", "package a\nclass User : Base()")
        project.withEnvironment { env ->
            val user = env.kotlinFiles(MAIN).single()
            env.superclassOf(MAIN, "a.User") shouldBe null
            val generated = "build/analysisCodeGen/staging/main/kotlin/a/Base.kt"
            val result =
                env.applyChanges(
                    SourceChangeSet(MAIN, mapOf(SourceFileId(generated) to "package a\nopen class Base"), emptyMap(), emptySet()),
                )
            result.affectedModules shouldBe setOf(MAIN)
            File(project.root, generated).readText() shouldBe "package a\nopen class Base"
            env.kotlinFiles(MAIN).map { it.name } shouldBe listOf("Base.kt", "User.kt")
            env.kotlinFiles(MAIN).single { it.name == "User.kt" } shouldBeSameInstanceAs user
            env.superclassOf(MAIN, "a.User") shouldBe "a.Base"
            val cls = user.collectDescendantsOfType<KtClassOrObject>().single()
            analyze(cls) { (cls.symbol as KaClassSymbol).superTypes.size } shouldBe 1
        }
    }

    "生成物を書き換えると環境を作り直し、新しい中身で解析する（比較用の Reference と同じ結果）" {
        listOf(StandaloneAnalysisEnvironmentFactory(), ReferenceAnalysisEnvironmentFactory()).forEach { factory ->
            val project = EngineProject()
            project.kotlin("a/User.kt", "package a\nclass User : Base()")
            project.withEnvironment(factory = factory) { env ->
                val path = SourceFileId("build/analysisCodeGen/staging/main/kotlin/a/Base.kt")
                env.applyChanges(SourceChangeSet(MAIN, mapOf(path to "package a\nopen class Base"), emptyMap(), emptySet()))
                env.applyChanges(
                    SourceChangeSet(
                        MAIN,
                        mapOf(
                            SourceFileId("build/analysisCodeGen/staging/main/kotlin/a/Root.kt") to "package a\nopen class Root",
                        ),
                        emptyMap(),
                        emptySet(),
                    ),
                )
                env.applyChanges(SourceChangeSet(MAIN, emptyMap(), mapOf(path to "package a\nopen class Base : Root()"), emptySet()))
                env.kotlinFiles(MAIN).map { it.name } shouldBe listOf("Base.kt", "Root.kt", "User.kt")
                env.superclassOf(MAIN, "a.Base") shouldBe "a.Root"
                env.superclassOf(MAIN, "a.User") shouldBe "a.Base"
            }
        }
    }
})
