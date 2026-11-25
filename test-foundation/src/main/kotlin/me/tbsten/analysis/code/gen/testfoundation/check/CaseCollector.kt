package me.tbsten.analysis.code.gen.testfoundation.check

import io.kotest.core.annotation.Ignored
import me.tbsten.analysis.code.gen.testfoundation.AcgSpec
import me.tbsten.analysis.code.gen.testfoundation.CaseDeclaration
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

/**
 * テストのクラス出力から [AcgSpec] のサブクラスを探し、生成して宣言を集める（テストは実行しない）。
 */
object CaseCollector {
    data class Result(val declarations: List<CaseDeclaration>, val problems: List<String>)

    /** クラス出力ディレクトリ群からクラス名を列挙し、現在のクラスローダで読む。 */
    fun collectFromClassDirs(
        classDirs: List<Path>,
        loader: ClassLoader = Thread.currentThread().contextClassLoader,
    ): Result {
        val names =
            classDirs.filter { it.isDirectory() }.flatMap { dir ->
                Files.walk(dir).use { s ->
                    s
                        .filter { it.extension == "class" }
                        .map {
                            it
                                .relativeTo(dir)
                                .toString()
                                .removeSuffix(".class")
                                .replace('/', '.')
                                .replace('\\', '.')
                        }.filter { !it.endsWith("module-info") }
                        .toList()
                }
            }
        val classes = names.sorted().mapNotNull { runCatching { Class.forName(it, false, loader) }.getOrNull() }
        return collect(classes)
    }

    fun collect(classes: List<Class<*>>): Result {
        val declarations = mutableListOf<CaseDeclaration>()
        val problems = mutableListOf<String>()
        for (c in classes) {
            if (!AcgSpec::class.java.isAssignableFrom(c)) continue
            if (Modifier.isAbstract(c.modifiers) || c.isInterface) continue
            if (c.isAnnotationPresent(Ignored::class.java)) {
                problems += "${c.name}: AcgSpec に @Ignored は使えない。保留は case(..., pending = PendingCase(...)) で表す"
                continue
            }
            val spec =
                try {
                    c.getDeclaredConstructor().also { it.isAccessible = true }.newInstance() as AcgSpec
                } catch (e: ReflectiveOperationException) {
                    val cause = (e.cause ?: e)
                    problems += "${c.name}: spec を生成できない（${cause::class.java.simpleName}: ${cause.message}）"
                    continue
                }
            declarations += spec.caseDeclarations
        }
        return Result(declarations, problems)
    }
}
