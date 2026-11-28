// PATTERN: CRM-T-0013
// TITLE: ChildOptionals / propertyFiltering / unpinnedGenericPropertySkippedWithWarning
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyFiltering
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--propertyFiltering/unpinnedGenericPropertySkippedWithWarning.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Tagged<M>(
    public val meta: M,
    public val label: String,
  ) : Source

  public object Loading : Source
}
