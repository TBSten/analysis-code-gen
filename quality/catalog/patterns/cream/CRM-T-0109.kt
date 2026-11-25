// PATTERN: CRM-T-0109
// TITLE: CombineMapping / map / singleMapping
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/06--map/singleMapping.md
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
  properties = [CombineMapping.Map(source = "sourceName", target = "targetName")],
)
public object Mapping

public data class SourceA(
  public val sourceName: String,
  public val shared: String,
)

public data class SourceB(
  public val extra: Int,
)

public data class Target(
  public val targetName: String,
  public val shared: String,
  public val extra: Int,
)
