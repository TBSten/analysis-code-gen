// PATTERN: CRM-T-0504
// TITLE: SealedCopy / visibility / visibilityOverrideInternal
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopySnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/08--visibility/visibilityOverrideInternal.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.SealedCopy

@SealedCopy(visibility = CopyVisibility.INTERNAL)
public sealed interface Source {
  public val name: String

  public data class Child(
    override val name: String,
  ) : Source
}
