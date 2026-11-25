// PATTERN: CRM-T-0108
// TITLE: CombineMapping / map / multipleMappings
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/06--map/multipleMappings.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
  properties = [CombineMapping.Map(source = "firstSource", target = "firstTarget"), CombineMapping.Map(source = "secondSource", target = "secondTarget")],
)
public object Mapping

public data class SourceA(
  public val firstSource: String,
  public val secondSource: Int,
)

public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val firstTarget: String,
  public val secondTarget: Int,
  public val extra: Int,
)
