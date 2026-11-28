// PATTERN: CRM-T-0483
// TITLE: SealedCopy / generics / multiTypeParamParent
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--generics/multiTypeParamParent.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source<K, V> {
  public val key: K

  public val `value`: V

  public data class Pair2<K, V>(
    override val key: K,
    override val `value`: V,
  ) : Source<K, V>
}
