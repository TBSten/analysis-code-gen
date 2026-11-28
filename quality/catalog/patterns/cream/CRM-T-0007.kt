// PATTERN: CRM-T-0007
// TITLE: ChildOptionals / hierarchyShape / transitiveNestedLeaves
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/transitiveNestedLeaves.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Root {
  public sealed interface Middle : Root {
    public data class Leaf(
      public val `data`: String,
    ) : Middle
  }

  public object Other : Root
}
