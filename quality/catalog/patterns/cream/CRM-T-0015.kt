// PATTERN: CRM-T-0015
// TITLE: ChildOptionals / parentOptionalInterop / overrideRenameOnParentVisibleProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=parentOptionalInterop
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--parentOptionalInterop/overrideRenameOnParentVisibleProperty.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.ParentOptional

@ChildOptionals
public sealed interface Source {
  public val `data`: String

  public data class Success(
    @ParentOptional(propertyName = "dataOrNull")
    override val `data`: String,
  ) : Source

  public data class Failure(
    override val `data`: String,
  ) : Source
}
