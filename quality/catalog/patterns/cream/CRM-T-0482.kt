// PATTERN: CRM-T-0482
// TITLE: SealedCopy / generics / extraTypeParamStarProjected
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--generics/extraTypeParamStarProjected.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source<T> {
  public val item: T

  public data class Tagged<T, M>(
    override val item: T,
    public val meta: M,
  ) : Source<T>
}
