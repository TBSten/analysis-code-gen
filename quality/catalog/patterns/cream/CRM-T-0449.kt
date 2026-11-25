// PATTERN: CRM-T-0449
// TITLE: ParentOptional / generics / boundedTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--generics/boundedTypeParam.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Comparable
import me.tbsten.cream.ParentOptional

public sealed interface Source<T : Comparable<T>> {
  public data class Filled<E : Comparable<E>>(
    @ParentOptional
    public val item: E,
  ) : Source<E>
}
