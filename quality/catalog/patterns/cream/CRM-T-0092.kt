// PATTERN: CRM-T-0092
// TITLE: CombineMapping / multiSource / mapAcrossSources
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/mapAcrossSources.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
  properties = [CombineMapping.Map(source = "originalB", target = "renamed")],
)
public object Mapping

public data class SourceA(
  public val fromA: String,
)

public data class SourceB(
  public val originalB: String,
  public val extra: Int,
)

public data class Target(
  public val fromA: String,
  public val renamed: String,
  public val extra: Int,
)
