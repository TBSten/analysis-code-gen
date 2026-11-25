// PATTERN: CRM-T-0219
// TITLE: CopyFrom / visibility / propertyVisibilities
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyFromSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--visibility/propertyVisibilities.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Target.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyFrom

@CopyFrom(Source::class)
public data class Target(
  public val publicProp: String,
  public val extra: String,
)

public data class Source(
  public val publicProp: String,
  internal val internalProp: Int,
  private val privateProp: Boolean,
)
