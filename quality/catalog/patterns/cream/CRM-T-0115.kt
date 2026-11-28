// PATTERN: CRM-T-0115
// TITLE: CombineMapping / visibility / visibilityOverridePublic
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--visibility/visibilityOverridePublic.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping
import me.tbsten.cream.CopyVisibility

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
  visibility = CopyVisibility.PUBLIC,
)
public object Mapping

public data class SourceA(
  public val name: String,
)

public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)
