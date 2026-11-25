// PATTERN: CRM-T-0509
// TITLE: SealedCopy / repeatable / stackedVariants
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=repeatable
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/10--repeatable/stackedVariants.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.NonCopyableStrategy
import me.tbsten.cream.SealedCopy

@SealedCopy(
  funName = "withUpdated",
  nonCopyableStrategy = NonCopyableStrategy.RETURN_AS_IS,
)
@SealedCopy(
  funName = "withUpdatedOrNull",
  nonCopyableStrategy = NonCopyableStrategy.RETURN_NULL,
)
public sealed interface Source {
  public val name: String

  public data class Loading(
    override val name: String,
  ) : Source

  public object Empty : Source {
    override val name: String = "empty"
  }
}
