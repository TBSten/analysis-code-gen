// PATTERN: CRM-T-0026
// TITLE: ChildOptionals / exclude / singleExcludedPropertySkipped
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/singleExcludedPropertySkipped.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Success(
    public val `data`: String,
    @ChildOptionals.Exclude
    public val trace: String,
  ) : Source

  public object Loading : Source
}
