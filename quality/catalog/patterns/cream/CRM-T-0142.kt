// PATTERN: CRM-T-0142
// TITLE: CombineTo / multiSource / mapAcrossSources
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/mapAcrossSources.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  public val primary: String,
)

@CombineTo(Target::class)
public data class SourceB(
  @CombineTo.Map("renamed")
  public val originalB: String,
)

public data class Target(
  public val primary: String,
  public val renamed: String,
)
