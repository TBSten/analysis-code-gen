package me.tbsten.analysiscodegen.e2e.support

/**
 * フィクスチャの中に、その場で書いた Native Processor のモジュールを作る（F-* の TP では足りないケース用）。
 *
 * ```
 * val p = emptyProject(includes = listOf(":processor")) {
 *     processorModule("processor", "p.HelloProvider", """
 *         package p
 *         import me.tbsten.analysiscodegen.processor.*
 *         class HelloProvider : AnalysisCodegenProcessorProvider { ... }
 *     """)
 * }
 * ```
 *
 * - analysis-code-gen-processor-api は compileOnly（実行時はエンジンが渡す。ADR-013）。
 *   `build/fixture-repo` に analysis-code-gen-processor-api が publish されている必要がある
 * - [dependencies] は `dependencies { }` の中にそのまま書く行
 */
fun FixtureProject.processorModule(
    dir: String,
    providerFqn: String,
    source: String,
    dependencies: List<String> = emptyList(),
    group: String = "example",
    version: String = "1.0",
) {
    write(
        "$dir/build.gradle.kts",
        """
        plugins { alias(libs.plugins.kotlin.jvm) }
        group = "$group"
        version = "$version"
        dependencies {
            compileOnly("me.tbsten.analysis.code.gen:analysis-code-gen-processor-api:${'$'}{acg.versions.acg.get()}")
        ${dependencies.joinToString("\n") { "    $it" }}
        }
        """,
    )
    write("$dir/src/main/kotlin/${providerFqn.substringBeforeLast('.').replace('.', '/')}/Processor.kt", source)
    write("$dir/src/main/resources/META-INF/services/me.tbsten.analysiscodegen.processor.AnalysisCodegenProcessorProvider", providerFqn)
}
