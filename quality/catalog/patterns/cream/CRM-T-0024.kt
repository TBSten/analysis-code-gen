// PATTERN: CRM-T-0024
// TITLE: ChildOptionals / exclude / excludedContributorDroppedFromMerge
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/excludedContributorDroppedFromMerge.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Success(
    @ChildOptionals.Exclude
    public val message: String,
    public val `data`: String,
  ) : Source

  public data class Failure(
    public val message: String,
  ) : Source
}
