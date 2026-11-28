// PATTERN: CRM-T-0459
// TITLE: ParentOptional / visibility / mergedNarrowestAnnotationWins
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--visibility/mergedNarrowestAnnotationWins.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional(visibility = CopyVisibility.INTERNAL)
    public val message: String,
  ) : Source

  public data class Failure(
    @ParentOptional
    public val message: String,
  ) : Source
}
