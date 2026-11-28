// PATTERN: CRM-T-0004
// TITLE: ChildOptionals / sealedParentKind / sealedInterfaceParent
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sealedParentKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/00--sealedParentKind/sealedInterfaceParent.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.ChildOptionals

@ChildOptionals
public sealed interface Source {
  public data class Child(
    public val `value`: Int,
  ) : Source

  public object Loading : Source
}
