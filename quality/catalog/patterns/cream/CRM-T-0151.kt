// PATTERN: CRM-T-0151
// TITLE: CombineTo / generics / targetOnlyTypeParam
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=generics
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/03--generics/targetOnlyTypeParam.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class Source(
  public val name: String,
)

public data class Target<T>(
  public val name: String,
  public val item: T,
)
