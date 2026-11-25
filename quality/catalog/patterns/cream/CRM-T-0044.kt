// PATTERN: CRM-T-0044
// TITLE: CombineFrom / multiSource / mapAcrossSources
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/mapAcrossSources.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom(
  SourceA::class,
  SourceB::class,
)
public data class Target(
  public val fromA: String,
  @CombineFrom.Map("originalB")
  public val renamed: String,
  public val extra: Int,
)

public data class SourceA(
  public val fromA: String,
)

public data class SourceB(
  public val originalB: String,
  public val extra: Int,
)
