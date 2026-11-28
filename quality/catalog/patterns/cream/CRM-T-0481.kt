// PATTERN: CRM-T-0481
// TITLE: SealedCopy / generics / concreteFixedTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--generics/concreteFixedTypeParam.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source<T> {
  public val item: T

  public data class Box<T>(
    override val item: T,
  ) : Source<T>

  public data class IntBox(
    override val item: Int,
  ) : Source<Int>
}
