// PATTERN: CRM-T-0309
// TITLE: CopyToChildren / visibility / visibilityOverrideInternal
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/06--visibility/visibilityOverrideInternal.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren
import me.tbsten.cream.CopyVisibility

@CopyToChildren(visibility = CopyVisibility.INTERNAL)
public sealed interface Source {
  public data class Child(
    public val name: String,
  ) : Source
}
