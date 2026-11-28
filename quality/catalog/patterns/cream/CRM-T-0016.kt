// PATTERN: CRM-T-0016
// TITLE: ChildOptionals / parentOptionalInterop / ownershipSplitAcrossAncestors
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=parentOptionalInterop
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--parentOptionalInterop/ownershipSplitAcrossAncestors.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.ParentOptional

@ChildOptionals
public sealed interface Root {
  public sealed interface Middle : Root {
    public data class Leaf(
      @ParentOptional
      public val `data`: String,
    ) : Middle
  }

  public object Other : Root
}
