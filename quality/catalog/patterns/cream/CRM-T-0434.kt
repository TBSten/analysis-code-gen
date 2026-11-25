// PATTERN: CRM-T-0434
// TITLE: ParentOptional / merge / mergeAcrossIntermediateAncestors
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=merge
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--merge/mergeAcrossIntermediateAncestors.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Root {
  public data class Direct(
    @ParentOptional
    public val id: String,
  ) : Root

  public sealed interface Middle : Root {
    public data class Leaf(
      @ParentOptional
      public val id: String,
    ) : Middle
  }
}
