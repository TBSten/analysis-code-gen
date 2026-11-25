package me.tbsten.analysiscodegen.e2e.support

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/** 実行された（UP-TO-DATE・FROM-CACHE・NO-SOURCE などを含む）タスクのパス。実行順。 */
fun BuildResult.taskPaths(): List<String> = tasks.map { it.path }

/** [outcome] で終わったタスクのパス。 */
fun BuildResult.taskPaths(outcome: TaskOutcome): List<String> = tasks.filter { it.outcome == outcome }.map { it.path }

/** `analysisCodeGen` で始まるタスクのパス。 */
fun BuildResult.analysisCodegenTaskPaths(): List<String> = taskPaths().filter { it.substringAfterLast(':').startsWith("analysisCodeGen") }

/**
 * `--dry-run` で [tasks] の実行計画を取り、タスクのパスを実行順に返す（タスクの依存関係を見るため。GRD-03 など）。
 */
fun FixtureProject.plannedTasks(vararg tasks: String): List<String> =
    build(*tasks, "--dry-run")
        .output
        .lineSequence()
        .map { it.trim() }
        .filter { it.startsWith(":") && it.endsWith(" SKIPPED") }
        .map { it.removeSuffix(" SKIPPED") }
        .toList()

/** ビルド出力から、`e: ` / `w: ` / `i: ` で始まる analysisCodeGen の診断行を集める（API-04 など）。 */
fun BuildResult.diagnosticLines(prefix: String): List<String> = output.lineSequence().filter { it.startsWith(prefix) }.toList()
