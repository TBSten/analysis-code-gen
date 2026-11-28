// PATTERN: CRM-T-0021
// TITLE: ChildOptionals / visibility / visibilityOverrideInternal
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--visibility/visibilityOverrideInternal.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.CopyVisibility

@ChildOptionals(visibility = CopyVisibility.INTERNAL)
public sealed interface Source {
  public data class Child(
    public val name: String,
  ) : Source

  public object Empty : Source
}
