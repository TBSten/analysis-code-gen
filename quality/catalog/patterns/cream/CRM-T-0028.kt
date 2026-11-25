// PATTERN: CRM-T-0028
// TITLE: ChildOptionals / propertyName / propertyOverridesSweepTemplate
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/07--propertyName/propertyOverridesSweepTemplate.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.DefaultAccessorPropertyName
import me.tbsten.cream.ParentOptional

@ChildOptionals(propertyName = DefaultAccessorPropertyName + "OrNull")
public sealed interface Source {
  public data class Success(
    public val `data`: String,
    @ParentOptional(propertyName = "resultCode")
    public val code: Int,
  ) : Source

  public object Loading : Source
}
