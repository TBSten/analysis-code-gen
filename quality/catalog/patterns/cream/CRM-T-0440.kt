// PATTERN: CRM-T-0440
// TITLE: ParentOptional / propertyShape / collectionProp
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/04--propertyShape/collectionProp.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import kotlin.collections.List
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Child(
    @ParentOptional
    public val tags: List<String>,
  ) : Source

  public object Empty : Source
}
