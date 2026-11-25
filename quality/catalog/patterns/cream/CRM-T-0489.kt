// PATTERN: CRM-T-0489
// TITLE: SealedCopy / propertyShape / mixedPrimitives
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--propertyShape/mixedPrimitives.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  public val name: String

  public val age: Int

  public val active: Boolean

  public val score: Double

  public data class Child(
    override val name: String,
    override val age: Int,
    override val active: Boolean,
    override val score: Double,
  ) : Source
}
