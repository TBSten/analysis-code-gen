// PATTERN: CRM-T-0075
// TITLE: CombineFrom / repeatable / duplicateSourceWithinOccurrence
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--repeatable/duplicateSourceWithinOccurrence.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom(
  SourceA::class,
  SourceA::class,
)
public data class Target(
  public val propertyA: String,
  public val propertyB: Int,
)

public data class SourceA(
  public val propertyA: String,
)
