// PATTERN: CRM-T-0435
// TITLE: ParentOptional / merge / subtypeChildrenMostDerivedFirst
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=merge
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--merge/subtypeChildrenMostDerivedFirst.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Source

public sealed class Middle(
  @ParentOptional(propertyName = "acc")
  public val x: String,
) : Source

public class Leaf(
  x: String,
  @ParentOptional(propertyName = "acc")
  public val y: String,
) : Middle(x)
