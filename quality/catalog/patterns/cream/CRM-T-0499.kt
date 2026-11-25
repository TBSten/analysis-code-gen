// PATTERN: CRM-T-0499
// TITLE: SealedCopy / exclude / excludeNoEffect
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/06--exclude/excludeNoEffect.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  @SealedCopy.Exclude
  public val tag: String
    get() = "tag"

  public data class Child(
    public val name: String,
  ) : Source
}
