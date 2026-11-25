// PATTERN: CRM-T-0455
// TITLE: ParentOptional / kdoc / descriptionAndExamples
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--kdoc/descriptionAndExamples.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.KDoc
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Child(
    @ParentOptional(kdoc = KDoc(description = "Read the payload without a cast.", examples = ["# Recommended\n\nval payload = state.data"]))
    public val `data`: String,
  ) : Source

  public object Empty : Source
}
