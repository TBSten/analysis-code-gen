// PATTERN: CRM-T-0450
// TITLE: ParentOptional / generics / chainedTypeParamRejected
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalSnapshotTest/All patterns/option=(notCopyToObject=true, defaultVisibility=INTERNAL)/05--generics/chainedTypeParamRejected.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Root.kt
package me.tbsten.cream.generated

import me.tbsten.cream.ParentOptional

public sealed interface Root<T>

public sealed interface Middle<E> : Root<E>

public data class Leaf<X>(
  @ParentOptional
  public val item: X,
) : Middle<X>
