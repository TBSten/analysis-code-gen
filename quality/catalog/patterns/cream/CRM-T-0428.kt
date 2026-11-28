// PATTERN: CRM-T-0428
// TITLE: ParentOptional / sealedParentKind / sealedClassParent
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sealedParentKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/00--sealedParentKind/sealedClassParent.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.ParentOptional

public sealed class Source {
  public data class Child(
    @ParentOptional
    public val `value`: Int,
  ) : Source()
}
