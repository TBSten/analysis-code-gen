// PATTERN: CRM-T-0316
// TITLE: CopyToChildren / funName / notCopyToObjectSingleLeafLiteralFunName
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--funName/notCopyToObjectSingleLeafLiteralFunName.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren(
  notCopyToObject = true,
  funName = "toState",
)
public sealed interface Source {
  public data class DataChild(
    public val a: String,
  ) : Source

  public object ObjectChild : Source
}
