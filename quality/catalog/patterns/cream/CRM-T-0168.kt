// PATTERN: CRM-T-0168
// TITLE: CombineTo / visibility / visibilityOverridePublic
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--visibility/visibilityOverridePublic.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineTo
import me.tbsten.cream.CopyVisibility

@CombineTo(
  Target::class,
  visibility = CopyVisibility.PUBLIC,
)
public data class SourceA(
  public val name: String,
)

@CombineTo(
  Target::class,
  visibility = CopyVisibility.PUBLIC,
)
public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)
