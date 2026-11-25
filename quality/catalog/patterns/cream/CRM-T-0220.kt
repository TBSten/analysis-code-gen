// PATTERN: CRM-T-0220
// TITLE: CopyFrom / visibility / visibilityOverrideInternal
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--visibility/visibilityOverrideInternal.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyFrom
import me.tbsten.cream.CopyVisibility

@CopyFrom(
  Source::class,
  visibility = CopyVisibility.INTERNAL,
)
public data class Target(
  public val name: String,
)

public data class Source(
  public val name: String,
)
