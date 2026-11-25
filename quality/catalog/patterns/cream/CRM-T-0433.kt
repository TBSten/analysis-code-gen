// PATTERN: CRM-T-0433
// TITLE: ParentOptional / merge / diamondChildOfTwoSealedInterfaces
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=merge
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/02--merge/diamondChildOfTwoSealedInterfaces.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Root.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.ParentOptional

public sealed interface Root

public sealed interface MidA : Root {
  @ParentOptional
  public val x: String
}

public sealed interface MidB : Root {
  @ParentOptional
  public val x: String
}

public class Leaf(
  override val x: String,
) : MidA,
    MidB
