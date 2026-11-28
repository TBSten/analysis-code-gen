// PATTERN: CRM-T-0486
// TITLE: SealedCopy / generics / varianceOutTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--generics/varianceOutTypeParam.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source<out T> {
  public val item: T

  public data class Filled<out T>(
    override val item: T,
  ) : Source<T>
}
