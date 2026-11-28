// PATTERN: CRM-T-0463
// TITLE: ParentOptional / deprecated / deprecatedChildClass
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=deprecated
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/08--deprecated/deprecatedChildClass.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Deprecated
import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  @Deprecated("legacy state")
  public class Legacy(
    @ParentOptional
    public val `value`: String,
  ) : Source

  public object Empty : Source
}
