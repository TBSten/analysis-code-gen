// PATTERN: CRM-T-0431
// TITLE: ParentOptional / hierarchyShape / multipleAccessorsOnOneParent
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/multipleAccessorsOnOneParent.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class First(
    @ParentOptional
    public val id: String,
  ) : Source

  public data class Second(
    @ParentOptional
    public val count: Int,
  ) : Source
}
