// PATTERN: CRM-T-0077
// TITLE: CombineFrom / repeatable / stackedAnnotationsDistinctFunNames
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/11--repeatable/stackedAnnotationsDistinctFunNames.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineFrom

@CombineFrom(
  SourceA::class,
  SourceB::class,
  funName = "toFoo",
)
@CombineFrom(
  SourceC::class,
  SourceD::class,
  funName = "toBar",
)
public data class Target(
  public val propertyA: String,
  public val propertyB: Int,
  public val propertyC: String,
  public val propertyD: Int,
)

public data class SourceA(
  public val propertyA: String,
)

public data class SourceB(
  public val propertyB: Int,
)

public data class SourceC(
  public val propertyC: String,
)

public data class SourceD(
  public val propertyD: Int,
)
