// PATTERN: CRM-T-0162
// TITLE: CombineTo / exclude / excludedProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/07--exclude/excludedProperty.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  public val name: String,
  @CombineTo.Exclude
  public val count: Int,
)

@CombineTo(Target::class)
public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val name: String,
  public val count: Int,
  public val extra: Int,
)
