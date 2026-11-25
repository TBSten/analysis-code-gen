// PATTERN: CRM-R-0078
// TITLE: cream.kt の test モジュール / VarargClasses
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/vararg/VarargClasses.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/vararg/VarargClasses.kt
package me.tbsten.cream.test.vararg

import me.tbsten.cream.CopyTo

// Regular (non-data) classes: `data class` forbids `vararg val` properties, so vararg copy
// targets are exercised here with plain classes (issue #49).
@CopyTo(VarargTarget::class)
class VarargSource(
    val id: Int,
    vararg val tags: String,
)

class VarargTarget(
    val id: Int,
    vararg val tags: String,
)

@CopyTo(PrimitiveVarargTarget::class)
class PrimitiveVarargSource(
    val id: Int,
    vararg val nums: Int,
)

class PrimitiveVarargTarget(
    val id: Int,
    vararg val nums: Int,
)

@CopyTo(ArrayToVarargTarget::class)
class ArrayToVarargSource(
    val id: Int,
    val tags: Array<String>,
)

class ArrayToVarargTarget(
    val id: Int,
    vararg val tags: String,
)
