// PATTERN: CRM-T-0323
// TITLE: CopyToChildren / map / mapToNonexistentProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--map/mapToNonexistentProperty.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface Source {
  @CopyToChildren.Map("missingTarget")
  public val sourceName: String

  public val shared: String

  public data class Child(
    public val targetName: String,
    override val shared: String,
  ) : Source {
    override val sourceName: String
      get() = targetName
  }
}
