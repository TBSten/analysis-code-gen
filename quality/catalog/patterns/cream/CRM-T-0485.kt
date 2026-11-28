// PATTERN: CRM-T-0485
// TITLE: SealedCopy / generics / sharedTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--generics/sharedTypeParam.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source<T> {
  public val item: T

  public data class Filled<T>(
    override val item: T,
  ) : Source<T>
}
