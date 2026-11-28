// PATTERN: CRM-T-0430
// TITLE: ParentOptional / hierarchyShape / intermediateSealedAncestors
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/intermediateSealedAncestors.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Root {
  public sealed interface Middle : Root {
    public data class Leaf(
      @ParentOptional
      public val `data`: String,
    ) : Middle
  }

  public object Other : Root
}
