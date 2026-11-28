// PATTERN: CRM-T-0029
// TITLE: ChildOptionals / propertyName / sweepWideTemplate
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--propertyName/sweepWideTemplate.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.DefaultAccessorPropertyName

@ChildOptionals(propertyName = DefaultAccessorPropertyName + "OrNull")
public sealed interface Source {
  public data class Success(
    public val `data`: String,
    public val count: Int,
  ) : Source

  public data class Failure(
    public val `data`: String,
  ) : Source

  public object Loading : Source
}
