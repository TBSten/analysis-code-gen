// PATTERN: CRM-T-0479
// TITLE: SealedCopy / hierarchyShape / transitiveNestedSealed
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/transitiveNestedSealed.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  public val id: String

  public data class Loading(
    override val id: String,
  ) : Source

  public sealed interface Branch : Source {
    public data class Done(
      override val id: String,
      public val note: String,
    ) : Branch

    public data class Pending(
      override val id: String,
    ) : Branch
  }
}
