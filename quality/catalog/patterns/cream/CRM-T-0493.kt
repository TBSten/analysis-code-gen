// PATTERN: CRM-T-0493
// TITLE: SealedCopy / nonCopyableStrategy / nonDataClassRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=nonCopyableStrategy
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/04--nonCopyableStrategy/nonDataClassRejected.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  public val name: String

  public data class Loading(
    override val name: String,
  ) : Source

  public class Frozen(
    override val name: String,
  ) : Source
}
