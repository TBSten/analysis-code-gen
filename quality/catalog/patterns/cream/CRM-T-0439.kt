// PATTERN: CRM-T-0439
// TITLE: ParentOptional / propertyName / renamedAccessor
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--propertyName/renamedAccessor.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional(propertyName = "dataOrNull")
    public val `data`: String,
  ) : Source

  public object Loading : Source
}
