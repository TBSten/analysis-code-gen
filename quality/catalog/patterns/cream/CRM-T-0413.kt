// PATTERN: CRM-T-0413
// TITLE: Multiple / invalidUsage / reportsEveryMisuseInOneRound.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @CopyToChildren annotation must be applied to a sealed class/interface, but multi.diag.NotSealedForChildren is not sealed (classKind: CLASS).
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/MultipleDiagnosticsTest/reportsEveryMisuseInOneRound.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: NotSealedForChildren.kt
package multi.diag

import me.tbsten.cream.CopyToChildren
import me.tbsten.cream.SealedCopy

@CopyToChildren
class NotSealedForChildren(val a: String)

@SealedCopy
class NotSealedForSelfCopy(val b: String)
