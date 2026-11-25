// PATTERN: CRM-T-0466
// TITLE: ParentOptional / deprecated / mergedFirstDeprecationWins
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=deprecated
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/08--deprecated/mergedFirstDeprecationWins.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Deprecated
import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public class Child1(
    @ParentOptional
    public val message: String,
  ) : Source

  public class Child2(
    @ParentOptional
    @Deprecated("second contributor deprecated")
    public val message: String,
  ) : Source

  public object Empty : Source
}
