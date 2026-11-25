// PATTERN: CRM-R-0050
// TITLE: cream.kt の test モジュール / AnnotationClassTarget
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/copyTo/AnnotationClassTarget.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/copyTo/AnnotationClassTarget.kt
package me.tbsten.cream.test.copyTo

import me.tbsten.cream.CopyTo

// data class -> annotation class copy (issue #132).
// Kotlin allows instantiating an annotation class via its constructor, so cream can build the
// target the same way it builds a regular class.
@CopyTo(AnnotationClassTarget::class)
data class AnnotationClassSource(
    val name: String,
    val count: Int,
)

annotation class AnnotationClassTarget(
    val name: String,
    val count: Int,
)
