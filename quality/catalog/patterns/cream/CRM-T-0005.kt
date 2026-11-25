// PATTERN: CRM-T-0005
// TITLE: ChildOptionals / hierarchyShape / leafPropertiesSweep
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=hierarchyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/01--hierarchyShape/leafPropertiesSweep.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Success(
    public val `data`: String,
  ) : Source

  public data class Failure(
    public val error: Int,
  ) : Source

  public object Loading : Source
}
