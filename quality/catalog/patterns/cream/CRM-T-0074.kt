// PATTERN: CRM-T-0074
// TITLE: CombineFrom / repeatable / duplicateOccurrenceRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--repeatable/duplicateOccurrenceRejected.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom(SourceA::class)
@CombineFrom(SourceA::class)
public data class Target(
  public val propertyA: String,
  public val propertyB: Int,
)

public data class SourceA(
  public val propertyA: String,
)
