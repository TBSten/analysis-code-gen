package me.tbsten.analysis.code.gen.enginetests.ver

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import me.tbsten.analysis.code.gen.testing.TestEngine
import me.tbsten.analysis.code.gen.testing.runCodegenTest
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

// VER-02・MEM-04・CLI の補助（ACG-46、ADR-037）。テストの中で Processor をコンパイルして JAR にする。
// テストの classpath に載っていないクラスにすることで、エンジンが Processor ごとに作る ClassLoader から読まれるようにする

/** Kotlin の標準ライブラリの JAR。[inherit] を切ったコンパイルに渡す。 */
internal val kotlinStdlib: File get() =
    File(
        KotlinVersion::class.java.protectionDomain.codeSource.location
            .toURI(),
    )

/**
 * [sources]（パス → 中身）をコンパイルし、クラスの出力ディレクトリを返す。Processor は動かさない（KSP2 の経路でコンパイルだけ）。
 * [inherit] が false なら、テストの classpath を引き継がず [classpath] だけを見る（古い analysis-code-gen-processor-api を作るため）。
 */
internal fun compileToClasses(
    sources: Map<String, String>,
    classpath: List<File> = emptyList(),
    inherit: Boolean = true,
): File {
    val result =
        runCodegenTest(TestEngine.Ksp2) {
            sources.forEach { (path, content) -> kotlin(path, content) }
            this.classpath += classpath
            inheritClassPath = inherit
        }
    withClue(result.messages) { result.isSuccess shouldBe true }
    // CodegenTestResult の ClassLoader は「出力ディレクトリ + classpath」の順に並ぶ（testing の実装、ADR-021）
    val urls = (result.classLoader as URLClassLoader).urLs
    return File(urls.first().toURI())
}

/** [classes] の中身と `META-INF/services/<サービス>` を入れた JAR を作る。 */
internal fun processorJar(
    classes: File,
    services: Map<String, List<String>>,
): File {
    val jar = Files.createTempFile("acg-compiled-processor", ".jar").toFile()
    JarOutputStream(jar.outputStream()).use { out ->
        classes.walkTopDown().filter { it.isFile }.forEach { f ->
            out.putNextEntry(JarEntry(f.relativeTo(classes).invariantSeparatorsPath))
            f.inputStream().use { it.copyTo(out) }
            out.closeEntry()
        }
        services.forEach { (service, impls) ->
            out.putNextEntry(JarEntry("META-INF/services/$service"))
            out.write(impls.joinToString("\n", postfix = "\n").toByteArray())
            out.closeEntry()
        }
    }
    return jar
}
