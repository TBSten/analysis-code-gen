// PATTERN: CRM-T-0399
// TITLE: CopyTo / visibility / internalSourceClass
// PROCESSORS: cream
// EXPECT: error
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=visibility
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/10--visibility/internalSourceClass.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
internal data class Source(
  public val name: String,
)

public data class Target(
  public val name: String,
  public val extra: Int,
)
