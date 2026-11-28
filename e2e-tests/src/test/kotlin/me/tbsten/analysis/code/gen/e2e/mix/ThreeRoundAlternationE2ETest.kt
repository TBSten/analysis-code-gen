package me.tbsten.analysis.code.gen.e2e.mix

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.e2e.support.emptyProject
import me.tbsten.analysis.code.gen.e2e.support.processorModule
import me.tbsten.analysis.code.gen.testfoundation.E2ESpec
import org.gradle.testkit.runner.TaskOutcome

// 5.16 MIX-03。設計文書 11.6 の 3 ラウンド交互を、Native と KSP の Processor を同じ SourceSet に並べた実際の Gradle ビルドで確かめる
// - ラウンド 1: KSP が `@Model class User` から `@Reg class UserMapper` を作る
// - ラウンド 2: Native が `@Reg` の付いた新しいファイルから `@Factory object UserRegistry` を作る
// - ラウンド 3: KSP が `@Factory` の付いた新しいシンボルから `GeneratedFactory` を作る
// 各 Processor は「誰が・どのラウンドで・何を作ったか」を warning で出し、その順番で交互に進んだことを見る

private val KSP_PROCESSOR = """
package kp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class AlternatingKspProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        object : SymbolProcessor {
            var round = 0

            override fun process(resolver: Resolver): List<KSAnnotated> {
                round++
                resolver.getSymbolsWithAnnotation("mix.Model").filterIsInstance<KSClassDeclaration>().forEach { cls ->
                    val name = cls.simpleName.asString() + "Mapper"
                    environment.codeGenerator.createNewFile(Dependencies(false, cls.containingFile!!), "mix.gen", name).use {
                        it.write("package mix.gen\n\n@mix.Reg\nclass ${'$'}name(val target: mix.User)\n".toByteArray())
                    }
                    environment.logger.warn("MIX03 ksp round=${'$'}round generated=${'$'}name")
                }
                resolver.getSymbolsWithAnnotation("mix.Factory").filterIsInstance<KSClassDeclaration>().forEach { cls ->
                    environment.codeGenerator.createNewFile(Dependencies(false, cls.containingFile!!), "mix.gen", "GeneratedFactory").use {
                        it.write("package mix.gen\n\nobject GeneratedFactory {\n    fun create() = UserRegistry.mapper\n}\n".toByteArray())
                    }
                    environment.logger.warn("MIX03 ksp round=${'$'}round generated=GeneratedFactory")
                }
                return emptyList()
            }
        }
}
"""

private val NATIVE_PROCESSOR = """
package np

import me.tbsten.analysis.code.gen.processor.*

class AlternatingNativeProvider : AnalysisCodegenProcessorProvider {
    override fun create(environment: AnalysisCodegenProcessorEnvironment) =
        object : AnalysisCodegenProcessor {
            override fun process(context: AnalysisCodegenRoundContext) {
                context.newFiles.filter { it.text.contains("@mix.Reg") }.forEach { f ->
                    context.codeGenerator.generateKotlin(
                        "mix.gen",
                        "UserRegistry",
                        Dependencies.isolating(f),
                        "package mix.gen\n\n@mix.Factory\nobject UserRegistry {\n    val mapper = UserMapper(mix.User(1))\n}\n",
                    )
                    environment.logger.warn("MIX03 native round=${'$'}{context.round} generated=UserRegistry")
                }
            }
        }
}
"""

class ThreeRoundAlternationE2ETest : E2ESpec({
    case("MIX-03", "設計文書 11.6 の 3 ラウンド交互のシナリオが実際の Gradle ビルドで通る") {
        val project =
            emptyProject(includes = listOf(":native-processor", ":ksp-processor")) {
                processorModule(
                    "native-processor",
                    "np.AlternatingNativeProvider",
                    NATIVE_PROCESSOR,
                    // KtFile（newFiles の要素）を解決するため。実行時はエンジンが渡す
                    dependencies = listOf("compileOnly(\"org.jetbrains.kotlin:kotlin-compiler:${'$'}{libs.versions.kotlin.get()}\")"),
                )
                write(
                    "ksp-processor/build.gradle.kts",
                    """
                    plugins { alias(libs.plugins.kotlin.jvm) }
                    group = "example"
                    version = "1.0"
                    dependencies { implementation(libs.ksp.api) }
                    """,
                )
                write("ksp-processor/src/main/kotlin/kp/Processor.kt", KSP_PROCESSOR)
                write(
                    "ksp-processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider",
                    "kp.AlternatingKspProvider",
                )
                write(
                    "build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.kotlin.jvm)
                        id("me.tbsten.analysis.code.gen")
                    }

                    analysisCodeGen {
                        sourceSets {
                            main {
                                processor(project(":native-processor"))
                                processor(project(":ksp-processor"))
                            }
                        }
                    }
                    """,
                )
                write(
                    "src/main/kotlin/mix/Model.kt",
                    """
                    package mix

                    annotation class Model
                    annotation class Reg
                    annotation class Factory

                    @Model
                    data class User(val id: Int)
                    """,
                )
                // 3 ラウンド目の生成物まで使うコード。生成が欠けると compileKotlin が落ちる
                write(
                    "src/main/kotlin/mix/Main.kt",
                    """
                    package mix

                    fun main() {
                        println(mix.gen.GeneratedFactory.create().target)
                    }
                    """,
                )
            }

        val result = project.build("compileKotlin")
        result.task(":analysisCodeGenMainSourceSet")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS

        // 交互に進んだ順番（ラウンド番号と作った Processor）
        val steps =
            Regex("""MIX03 (ksp|native) round=(\d+) generated=(\w+)""")
                .findAll(result.output)
                .map {
                    it.value.substringAfter("MIX03 ")
                }.toList()
        steps shouldBe
            listOf(
                "ksp round=1 generated=UserMapper",
                "native round=2 generated=UserRegistry",
                "ksp round=3 generated=GeneratedFactory",
            )

        // 3 つの生成物が同じ出力先に並ぶ（ADR-016）
        project.generated("main").sourceNames() shouldContainAll listOf("UserMapper", "UserRegistry", "GeneratedFactory")
    }
})
