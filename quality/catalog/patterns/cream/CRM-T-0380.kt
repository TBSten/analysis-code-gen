// PATTERN: CRM-T-0380
// TITLE: CopyTo / propertyShape / mixedPrimitives
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/05--propertyShape/mixedPrimitives.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val name: String,
  public val age: Int,
  public val active: Boolean,
  public val score: Double,
)

public data class Target(
  public val name: String,
  public val age: Int,
  public val active: Boolean,
  public val score: Double,
)
