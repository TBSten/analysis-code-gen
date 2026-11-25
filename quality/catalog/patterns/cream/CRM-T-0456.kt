// PATTERN: CRM-T-0456
// TITLE: ParentOptional / kdoc / mergedKdocUsesFirstEntry
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--kdoc/mergedKdocUsesFirstEntry.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.KDoc
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional(kdoc = KDoc(description = "First contributor's doc."))
    public val message: String,
  ) : Source

  public data class Failure(
    @ParentOptional(kdoc = KDoc(description = "Second contributor's doc."))
    public val message: String,
  ) : Source
}
