// PATTERN: CRM-T-0027
// TITLE: ChildOptionals / propertyName / bareParentOptionalInheritsSweepArguments
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--propertyName/bareParentOptionalInheritsSweepArguments.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.DefaultAccessorPropertyName
import me.tbsten.cream.ParentOptional

@ChildOptionals(
  propertyName = DefaultAccessorPropertyName + "OrNull",
  visibility = CopyVisibility.INTERNAL,
)
public sealed interface Source {
  public data class Success(
    @ParentOptional
    public val `data`: String,
  ) : Source

  public object Loading : Source
}
