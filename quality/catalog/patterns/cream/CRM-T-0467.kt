// PATTERN: CRM-T-0467
// TITLE: RunCompileSnapshotTestSmokeTest / misc / compiles a @CopyTo FileSpec, exposes the result to assertions, and returns it
// PROCESSORS: cream
// EXPECT: ok
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/RunCompileSnapshotTestSmokeTest/compiles a @CopyTo FileSpec, exposes the result to assertions, and returns it.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Source.kt
package smoke.compile

import kotlin.Int
import kotlin.String
import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
public data class Source(
  public val shared: String,
)

public data class Target(
  public val shared: String,
  public val extra: Int,
)
