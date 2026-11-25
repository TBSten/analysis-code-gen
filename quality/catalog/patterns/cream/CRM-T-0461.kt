// PATTERN: CRM-T-0461
// TITLE: ParentOptional / visibility / visibilityOverridePublicOnInternalChild
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--visibility/visibilityOverridePublicOnInternalChild.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public object Empty : Source
}

internal data class Child(
  @ParentOptional(visibility = CopyVisibility.PUBLIC)
  public val `data`: String,
) : Source
