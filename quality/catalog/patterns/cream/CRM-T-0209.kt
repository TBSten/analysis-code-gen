// PATTERN: CRM-T-0209
// TITLE: CopyFrom / map / copyToMapOnSourceDoesNotApply
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/07--map/copyToMapOnSourceDoesNotApply.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyFrom
import me.tbsten.cream.CopyTo

@CopyFrom(Source::class)
public data class Target(
  public val targetName: String,
  public val shared: String,
)

public data class Source(
  @CopyTo.Map("targetName")
  public val sourceName: String,
  public val shared: String,
)
