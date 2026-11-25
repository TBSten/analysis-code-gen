package me.tbsten.analysiscodegen.e2e.gcc

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.tbsten.analysiscodegen.gradle.AnalysisCodegenExtension
import me.tbsten.analysiscodegen.gradle.AnalysisCodegenTask
import me.tbsten.analysiscodegen.testfoundation.Milestone
import me.tbsten.analysiscodegen.testfoundation.PendingCase
import me.tbsten.analysiscodegen.testfoundation.UnitSpec
import org.gradle.api.Project
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files

// 5.13 GCC-04。Build Cache の relocatable（GCC-02）の前提を、タスクの宣言と設定後の入力の値で確かめる（U）

/** AnalysisCodegenTask（とその親）のファイル入力の getter。 */
private fun fileInputGetters(): List<Method> =
    generateSequence<Class<*>>(AnalysisCodegenTask::class.java) { it.superclass }
        .takeWhile { it != Any::class.java }
        .flatMap { it.declaredMethods.asSequence() }
        .filter { m ->
            m.isAnnotationPresent(InputFiles::class.java) ||
                m.isAnnotationPresent(InputDirectory::class.java) ||
                m.isAnnotationPresent(InputFile::class.java) ||
                m.isAnnotationPresent(Classpath::class.java) ||
                m.isAnnotationPresent(CompileClasspath::class.java)
        }.toList()

/** kotlin("jvm") + analysisCodeGen を適用した、[dir] に置いたプロジェクト。 */
private fun configuredProject(dir: File): Project {
    dir.resolve("src/main/kotlin/com/example").mkdirs()
    dir.resolve("src/main/kotlin/com/example/A.kt").writeText("package com.example\n\nclass A\n")
    val project =
        ProjectBuilder
            .builder()
            .withProjectDir(dir)
            .withName("app")
            .build()
    project.pluginManager.apply("org.jetbrains.kotlin.jvm")
    project.pluginManager.apply("me.tbsten.analysiscodegen")
    project.extensions.getByType(AnalysisCodegenExtension::class.java).arg("key", "value")
    return project
}

/** 設定後のタスクの、パスでない入力（@Input）の値。 */
private fun AnalysisCodegenTask.plainInputs(): Map<String, Any?> =
    mapOf(
        "unitName" to unitName.orNull,
        "options" to options.orNull,
    )

class TaskInputPathTest : UnitSpec({
    case("GCC-04", "タスク入力のパスが相対で扱われ、キャッシュキーに絶対パスが入らない", pending = PendingCase(Milestone.M2)) {
        // 1. ファイル入力はすべて RELATIVE（か NAME_ONLY / NONE）か、classpath として正規化される
        val getters = fileInputGetters()
        getters.shouldNotBeEmpty()
        val absolute =
            getters.filter { m ->
                val classpath = m.isAnnotationPresent(Classpath::class.java) || m.isAnnotationPresent(CompileClasspath::class.java)
                val sensitivity = m.getAnnotation(PathSensitive::class.java)?.value
                !classpath && (sensitivity == null || sensitivity == PathSensitivity.ABSOLUTE)
            }
        absolute.map { it.name }.shouldBeEmpty()

        // 2. 場所の違う 2 つのプロジェクトで、@Input の値が同じで、どちらのディレクトリの絶対パスも含まない
        val dirA = Files.createTempDirectory("acg-gcc04-a").toFile()
        val dirB = Files.createTempDirectory("acg-gcc04-b").toFile()
        val tasksA = configuredProject(dirA).tasks.withType(AnalysisCodegenTask::class.java).toList()
        val tasksB = configuredProject(dirB).tasks.withType(AnalysisCodegenTask::class.java).toList()
        tasksA.shouldNotBeEmpty()
        tasksA.map { it.name }.sorted() shouldBe tasksB.map { it.name }.sorted()
        for (a in tasksA) {
            val b = tasksB.single { it.name == a.name }
            a.plainInputs() shouldBe b.plainInputs()
            val text = a.plainInputs().toString()
            listOf(dirA, dirB).map { it.absolutePath }.filter { text.contains(it) }.shouldBeEmpty()
        }
    }
})
