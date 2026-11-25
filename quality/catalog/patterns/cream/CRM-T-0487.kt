// PATTERN: CRM-T-0487
// TITLE: SealedCopy / propertyShape / collectionProp
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--propertyShape/collectionProp.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.collections.List
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  public val tags: List<String>

  public data class Child(
    override val tags: List<String>,
  ) : Source
}
