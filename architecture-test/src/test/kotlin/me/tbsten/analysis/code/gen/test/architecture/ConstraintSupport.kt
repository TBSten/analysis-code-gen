@file:OptIn(ExperimentalKatachiApi::class)

package me.tbsten.analysis.code.gen.test.architecture

import java.io.File
import me.tbsten.katachi.ExperimentalKatachiApi
import me.tbsten.katachi.dsl.FileConstraintFailure
import me.tbsten.katachi.dsl.FileConstraintFix
import me.tbsten.katachi.dsl.FileConstraintReason
import me.tbsten.katachi.dsl.FileConstraintScope

/** Kotlin コンパイラと AA（org.jetbrains.kotlin.analysis.api）。AA に依存してよいモジュールだけが import する */
const val KOTLIN_COMPILER = "org.jetbrains.kotlin.**"

/** AA が前提とする IntelliJ Platform */
const val INTELLIJ = "com.intellij.**"

/**
 * 上流から写したファイルは、上流のヘッダの直後に `// Ported-From:` を持つ（ADR-018、`.claude/rules/porting.md`）。
 * 中身の正しさ（SHA・port-map.csv との一致）は scripts/lic01/check_provenance.py が見る。ここでは Role に置いた時点で出所があることを守る。
 * 上流の testData はヘッダの後ろに複数ファイルの区切りが続くので、行の位置は決めずにファイル全体から探す
 */
fun FileConstraintScope.portedFromHeader() = fileConstraint("上流から写したファイルは // Ported-From: で出所を書く") { subject ->
    subject.files.mapNotNull { file ->
        val hasHeader = File(subject.projectRoot, file).useLines { lines -> lines.any { it.startsWith("// Ported-From: ") } }
        if (hasHeader) return@mapNotNull null
        FileConstraintFailure(
            file = file,
            reasons = listOf(FileConstraintReason("Missing", "// Ported-From: <owner>/<repo>@<40 桁 SHA>:<上流パス>")),
            howToFix = listOf(FileConstraintFix("skill port-from-ksp2 の手順で、上流のヘッダの直後に出所の 3 行を足す")),
        )
    }
}

/**
 * 各モジュールの build.gradle.kts は convention plugin（`id("acg.*")`）を使い、共通設定を個別に書かない（`.claude/rules/kotlin-code.md`）。
 * 例外: ルート（Dokka の集約だけ）、:quality（検査タスクだけの java モジュール）、:architecture-test（katachi scaffold が書いたもの）
 */
fun FileConstraintScope.conventionPluginRequired() = fileConstraint("モジュールの build.gradle.kts は convention plugin を使う") { subject ->
    subject.files
        .filterNot { it in CONVENTION_PLUGIN_EXEMPT }
        .mapNotNull { file ->
            val text = File(subject.projectRoot, file).readText()
            if (CONVENTION_PLUGIN.containsMatchIn(text)) return@mapNotNull null
            FileConstraintFailure(
                file = file,
                reasons = listOf(FileConstraintReason("Missing", "plugins { id(\"acg.<name>\") }")),
                howToFix = listOf(FileConstraintFix("build-logic の acg.jvm / acg.library / acg.gradle-plugin / acg.tests のどれかを plugins { } に足す")),
            )
        }
}

private val CONVENTION_PLUGIN = Regex("""id\("acg\.[a-z-]+"\)""")

private val CONVENTION_PLUGIN_EXEMPT = setOf("build.gradle.kts", "quality/build.gradle.kts", "architecture-test/build.gradle.kts")
