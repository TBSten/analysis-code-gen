// PATTERN: CRM-T-0391
// TITLE: CopyTo / exclude / genericExclude
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=exclude
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/08--exclude/genericExclude.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source<T>(
  public val name: String,
  @CopyTo.Exclude
  public val item: T,
)

public data class Target<T>(
  public val name: String,
  public val item: T,
)
