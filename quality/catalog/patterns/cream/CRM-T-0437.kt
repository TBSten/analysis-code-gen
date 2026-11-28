// PATTERN: CRM-T-0437
// TITLE: ParentOptional / propertyName / derivedFromDefaultToken
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=propertyName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--propertyName/derivedFromDefaultToken.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.DefaultAccessorPropertyName
import me.tbsten.cream.ParentOptional

public sealed interface Source {
  public data class Success(
    @ParentOptional(propertyName = DefaultAccessorPropertyName + "OrNull")
    public val `data`: String,
    @ParentOptional(propertyName = DefaultAccessorPropertyName + "OrNull")
    public val count: Int,
  ) : Source

  public object Loading : Source
}
