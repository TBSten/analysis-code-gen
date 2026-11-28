// PATTERN: CRM-T-0307
// TITLE: CopyToChildren / kdoc / descriptionAndExamples
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=kdoc
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/05--kdoc/descriptionAndExamples.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren
import me.tbsten.cream.KDoc

@CopyToChildren(kdoc = KDoc(description = "Use this only when migrating.", examples = ["# Recommended\n\nval target = source.copyToSourceChild(name = name)"]))
public sealed interface Source {
  public data class Child(
    public val name: String,
  ) : Source
}
