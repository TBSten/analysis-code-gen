// PATTERN: CRM-T-0454
// TITLE: ParentOptional / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.KDoc
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Child(
    @ParentOptional(kdoc = KDoc(description = "Custom description for the accessor."))
    public val `data`: String,
  ) : Source

  public object Empty : Source
}
