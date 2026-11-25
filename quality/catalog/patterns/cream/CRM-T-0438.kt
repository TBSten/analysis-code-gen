// PATTERN: CRM-T-0438
// TITLE: ParentOptional / propertyName / renameAvoidsMerge
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--propertyName/renameAvoidsMerge.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional
    public val message: String,
  ) : Source

  public data class Failure(
    @ParentOptional(propertyName = "failureMessage")
    public val message: String,
  ) : Source
}
