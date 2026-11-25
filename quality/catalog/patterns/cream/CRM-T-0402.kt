// PATTERN: CRM-T-0402
// TITLE: CopyTo / visibility / visibilityOverridePublic
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--visibility/visibilityOverridePublic.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTo
import me.tbsten.cream.CopyVisibility

@CopyTo(
  Target::class,
  visibility = CopyVisibility.PUBLIC,
)
public data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
)
