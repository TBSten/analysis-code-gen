// PATTERN: CRM-T-0062
// TITLE: CombineFrom / map / multipleMappings
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/06--map/multipleMappings.md
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
  @CombineFrom.Map("a")
  public val first: String,
  @CombineFrom.Map("b")
  public val second: String,
  public val extra: Int,
)

public data class SourceA(
  public val a: String,
  public val b: String,
)

public data class SourceB(
  public val extra: Int,
)
