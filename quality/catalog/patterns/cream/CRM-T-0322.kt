// PATTERN: CRM-T-0322
// TITLE: CopyToChildren / map / mapToDifferentNamePerChild
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=map
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToChildrenSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/09--map/mapToDifferentNamePerChild.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyToChildren

@CopyToChildren
public sealed interface Source {
  @CopyToChildren.Map(
    "idA",
    "idB",
  )
  public val sourceId: String

  public data class ChildA(
    public val idA: String,
  ) : Source {
    override val sourceId: String
      get() = idA
  }

  public data class ChildB(
    public val idB: String,
  ) : Source {
    override val sourceId: String
      get() = idB
  }
}
