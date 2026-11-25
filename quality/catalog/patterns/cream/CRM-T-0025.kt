// PATTERN: CRM-T-0025
// TITLE: ChildOptionals / exclude / parentOptionalOverridesExclude
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/parentOptionalOverridesExclude.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.ParentOptional

@ChildOptionals
public sealed interface Source {
  public data class Paid(
    @ParentOptional
    @ChildOptionals.Exclude
    public val amount: Int,
  ) : Source

  public object Unpaid : Source
}
