// PATTERN: CRM-T-0311
// TITLE: CopyToChildren / notCopyToObject / allObjectsSuppressedEmptyFile
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=notCopyToObject
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/07--notCopyToObject/allObjectsSuppressedEmptyFile.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import me.tbsten.cream.CopyToChildren

@CopyToChildren(notCopyToObject = true)
public sealed interface Source {
  public object First : Source

  public object Second : Source
}
