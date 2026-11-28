// PATTERN: CRM-T-0143
// TITLE: CombineTo / multiSource / multiSourceGenerics
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=multiSource
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CombineToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/02--multiSource/multiSourceGenerics.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: SourceA.kt
package me.tbsten.cream.generated

import me.tbsten.cream.CombineTo

@CombineTo(Target::class)
public data class SourceA<T>(
  public val a: T,
)

@CombineTo(Target::class)
public data class SourceB<U>(
  public val b: U,
)

public data class Target<T, U>(
  public val a: T,
  public val b: U,
)
