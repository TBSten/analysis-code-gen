// PATTERN: CRM-T-0215
// TITLE: CopyFrom / exclude / mapAndExclude
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--exclude/mapAndExclude.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
public data class Target(
  @CopyFrom.Map("sourceName")
  @CopyFrom.Exclude
  public val targetName: String,
  public val shared: String,
)

public data class Source(
  public val sourceName: String,
  public val shared: String,
)
