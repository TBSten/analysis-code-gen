// PATTERN: CRM-T-0014
// TITLE: ChildOptionals / parentOptionalInterop / intermediateSealedOwnProperty
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=parentOptionalInterop
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/03--parentOptionalInterop/intermediateSealedOwnProperty.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.ChildOptionals
import me.tbsten.cream.ParentOptional

@ChildOptionals
public sealed interface Source {
  public object Other : Source
}

public sealed class Middle(
  @ParentOptional
  public val session: String,
) : Source

public class Leaf(
  session: String,
  public val extra: Int,
) : Middle(session)
