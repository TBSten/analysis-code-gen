// PATTERN: CRM-R-0072
// TITLE: cream.kt の test モジュール / GenericClasses
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: real:TBSten/cream@2162ec8d5261/test/src/commonMain/kotlin/me/tbsten/cream/test/generic/GenericClasses.kt
// LICENSE: Apache-2.0
// STATUS: candidate
// NOTE: cream.kt は外部の利用プロジェクトが無いので、KMP の統合テスト用モジュールを源 ② の代わりにする（ADR-019・ADR-020）。同じモジュールの他ファイルに依存する場合がある
// FILE: commonMain/me/tbsten/cream/test/generic/GenericClasses.kt
package me.tbsten.cream.test.generic

import me.tbsten.cream.CopyFrom
import me.tbsten.cream.CopyTo

@CopyTo(GenericTargetWithThreeTypeArg::class)
@CopyFrom(GenericTargetWithThreeTypeArg::class)
data class GenericSourceWithTwoTypeArg<
    @CopyFrom.Map("Aaa")
    @CopyTo.Map("Aaa")
    A,
    @CopyFrom.Map("Bbb")
    @CopyTo.Map("Bbb")
    B,
>(
    val a: A?,
    val b: List<B>,
    val d: Int,
) where A : Comparable<String>, A : CharSequence

data class GenericTargetWithThreeTypeArg<Aaa, Bbb, Ccc>(
    val a: Aaa?,
    val b: List<Bbb>,
    val c: Ccc,
    val d: Int,
) where Aaa : Comparable<String>, Aaa : CharSequence

@CopyTo(GenericTargetWithTwoTypeArg::class, GenericTargetObject::class)
@CopyFrom(GenericTargetWithTwoTypeArg::class)
data class GenericSourceWithThreeTypeArg<
    @CopyFrom.Map("Aaa")
    A,
    @CopyFrom.Map("Bbb")
    B,
    C,
>(
    val a: A?,
    val b: List<B>,
    val c: C,
) where A : Comparable<String>, A : CharSequence

data class GenericTargetWithTwoTypeArg<Aaa, Bbb>(
    val a: Aaa?,
    val b: List<Bbb>,
) where Aaa : Comparable<String>, Aaa : CharSequence

data object GenericTargetObject
