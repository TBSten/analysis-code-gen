// PATTERN: CRM-T-0315
// TITLE: CopyToChildren / funName / literalFunNameRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=funName
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--funName/literalFunNameRejected.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren(funName = "toState")
public sealed interface Source {
  public val id: String

  public data class Loading(
    override val id: String,
  ) : Source

  public data class Done(
    override val id: String,
    public val `data`: String,
  ) : Source
}
