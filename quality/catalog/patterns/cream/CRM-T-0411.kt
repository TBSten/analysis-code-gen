// PATTERN: CRM-T-0411
// TITLE: CopyTo / escaping / spaceInName
// PROCESSORS: cream
// EXPECT: ok
// OPTIONS: cream.copyFunNamePrefix=to, cream.escapeDot=replace-to-underscore
// OPTIONS: cream.copyFunNamingStrategy=inner-name, cream.notCopyToObject=true, cream.defaultVisibility=INTERNAL
// TAGS: cream.family=escaping
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/CopyToSnapshotTest/All patterns/option=(prefix=to, escapeDot=replace-to-underscore)/13--escaping/spaceInName.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package me.tbsten.cream.generated

import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val `my value`: String,
)

public data class Target(
  public val `my value`: String,
)
