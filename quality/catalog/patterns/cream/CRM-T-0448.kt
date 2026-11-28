// PATTERN: CRM-T-0448
// TITLE: ParentOptional / propertyShape / typealiasPreserved
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/04--propertyShape/typealiasPreserved.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public typealias UserId = String

public sealed interface Source {
  public data class Child(
    @ParentOptional
    public val id: UserId,
  ) : Source

  public object Empty : Source
}
