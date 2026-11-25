// PATTERN: CRM-T-0010
// TITLE: ChildOptionals / propertyFiltering / parentVisiblePropertiesSkipped
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyFiltering
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--propertyFiltering/parentVisiblePropertiesSkipped.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public val shared: String

  public data class Success(
    override val shared: String,
    public val `data`: String,
  ) : Source

  public data class Failure(
    override val shared: String,
  ) : Source
}
