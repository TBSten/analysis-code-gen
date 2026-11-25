// PATTERN: CRM-T-0148
// TITLE: CombineTo / generics / boundedTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/03--generics/boundedTypeParam.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Comparable
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class Source<T : Comparable<T>>(
  public val item: T,
)

public data class Target<T : Comparable<T>>(
  public val item: T,
)
