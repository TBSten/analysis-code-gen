// PATTERN: CRM-T-0012
// TITLE: ChildOptionals / propertyFiltering / privatePropertiesSkippedSilently
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyFiltering
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--propertyFiltering/privatePropertiesSkippedSilently.md
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
    private val secret: String,
  ) : Source

  public object Loading : Source
}
