// PATTERN: CRM-T-0497
// TITLE: SealedCopy / map / mappedNonDataChild
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--map/mappedNonDataChild.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.SealedCopy

@SealedCopy
public sealed interface Source {
  public val name: String

  public class Custom(
    override val name: String,
  ) : Source {
    @SealedCopy.Via
    public fun cloneWith(name: String = this.name): Custom = Custom(name)
  }
}
