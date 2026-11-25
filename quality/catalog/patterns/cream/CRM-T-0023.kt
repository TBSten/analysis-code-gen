// PATTERN: CRM-T-0023
// TITLE: ChildOptionals / exclude / excludeNoEffectWarns
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/excludeNoEffectWarns.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Success(
    public val `data`: String,
  ) : Source

  public object Loading : Source
}

public data class Unrelated(
  @ChildOptionals.Exclude
  public val ignored: String,
)
