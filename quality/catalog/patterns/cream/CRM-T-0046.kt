// PATTERN: CRM-T-0046
// TITLE: CombineFrom / multiSource / noSourcesRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/noSourcesRejected.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom
public data class Target(
  public val name: String,
  public val extra: Int,
)
