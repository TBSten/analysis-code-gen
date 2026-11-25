// PATTERN: CRM-T-0043
// TITLE: CombineFrom / multiSource / excludeOverlappingProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/excludeOverlappingProperty.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom(
  SourceA::class,
  SourceB::class,
)
public data class Target(
  @CombineFrom.Exclude
  public val shared: String,
  public val uniqueA: Int,
  public val uniqueB: Boolean,
)

public data class SourceA(
  public val shared: String,
  public val uniqueA: Int,
)

public data class SourceB(
  public val shared: String,
  public val uniqueB: Boolean,
)
