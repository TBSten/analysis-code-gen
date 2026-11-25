// PATTERN: CRM-T-0154
// TITLE: CombineTo / propertyShape / mixedPrimitives
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/04--propertyShape/mixedPrimitives.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: SourceA.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA(
  public val name: String,
  public val age: Int,
)

@CombineTo(Target::class)
public data class SourceB(
  public val active: Boolean,
  public val score: Double,
)

public data class Target(
  public val name: String,
  public val age: Int,
  public val active: Boolean,
  public val score: Double,
)
