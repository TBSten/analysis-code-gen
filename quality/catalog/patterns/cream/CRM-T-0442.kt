// PATTERN: CRM-T-0442
// TITLE: ParentOptional / propertyShape / delegatedProp
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyShape
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/04--propertyShape/delegatedProp.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public class Child : Source {
    @ParentOptional
    public val lazyValue: Int by lazy { 42 }
  }

  public object Empty : Source
}
