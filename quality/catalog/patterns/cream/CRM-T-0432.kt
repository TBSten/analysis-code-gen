// PATTERN: CRM-T-0432
// TITLE: ParentOptional / hierarchyShape / siblingsWithoutAnnotation
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/siblingsWithoutAnnotation.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional
    public val `data`: String,
  ) : Source

  public data class Failure(
    public val error: Int,
  ) : Source

  public object Loading : Source
}
