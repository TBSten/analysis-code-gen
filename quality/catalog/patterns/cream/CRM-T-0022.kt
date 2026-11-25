// PATTERN: CRM-T-0022
// TITLE: ChildOptionals / exclude / allContributorsExcludedNoAccessor
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/allContributorsExcludedNoAccessor.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Success(
    @ChildOptionals.Exclude
    public val token: String,
    public val `data`: String,
  ) : Source

  public data class Failure(
    @ChildOptionals.Exclude
    public val token: String,
    public val error: String,
  ) : Source
}
