// PATTERN: CRM-T-0018
// TITLE: ChildOptionals / kdoc / description
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/04--kdoc/description.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.KDoc

@ChildOptionals(kdoc = KDoc(description = "Custom description for the swept accessors."))
public sealed interface Source {
  public data class Child(
    public val name: String,
  ) : Source

  public object Empty : Source
}
