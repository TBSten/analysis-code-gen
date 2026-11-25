// PATTERN: CRM-T-0120
// TITLE: CombineMapping / sourceKindValidation / nonClassSource
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKindValidation
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--sourceKindValidation/nonClassSource.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class, SourceB::class],
  target = Target::class,
)
public object Mapping

public data class SourceA(
  public val name: String,
)

public sealed interface SourceB {
  public val extra: String
}

public data class Target(
  public val name: String,
  public val extra: String,
)
