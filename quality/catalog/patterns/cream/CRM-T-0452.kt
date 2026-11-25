// PATTERN: CRM-T-0452
// TITLE: ParentOptional / generics / genericLeafUnderIndirectAncestor
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--generics/genericLeafUnderIndirectAncestor.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Root

public sealed interface Mid : Root

public data class Leaf<T>(
  @ParentOptional
  public val label: String,
  public val `value`: T,
) : Mid
