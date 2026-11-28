// PATTERN: CRM-T-0453
// TITLE: ParentOptional / generics / multiBoundWhereClause
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--generics/multiBoundWhereClause.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.CharSequence
import kotlin.Comparable
import me.tbsten.cream.ParentOptional

public sealed interface Source<T> where T : Comparable<T>, T : CharSequence {
  public data class Filled<E>(
    @ParentOptional
    public val item: E,
  ) : Source<E> where E : Comparable<E>, E : CharSequence
}
