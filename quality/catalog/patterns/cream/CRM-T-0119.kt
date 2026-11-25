// PATTERN: CRM-T-0119
// TITLE: CombineMapping / sourceKindValidation / insufficientSources
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sourceKindValidation
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineMappingSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--sourceKindValidation/insufficientSources.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Mapping.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineMapping

@CombineMapping(
  sources = [SourceA::class],
  target = Target::class,
)
public object Mapping

public data class SourceA(
  public val name: String,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)
