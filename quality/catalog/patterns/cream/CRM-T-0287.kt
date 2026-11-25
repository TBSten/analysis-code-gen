// PATTERN: CRM-T-0287
// TITLE: CopyToChildren / sealedParentKind / sealedClassParent
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=sealedParentKind
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/00--sealedParentKind/sealedClassParent.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed class Source {
  public data class Child(
    public val `value`: Int,
  ) : Source()
}
