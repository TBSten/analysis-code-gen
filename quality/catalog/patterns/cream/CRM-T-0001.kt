// PATTERN: CRM-T-0001
// TITLE: ChildOptionals / invalidUsage / childOptionalsOnNonSealed.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ChildOptionals must be applied to a sealed class/interface, but diag.co.NotSealed is not sealed (classKind: CLASS).
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsInvalidUsageTest/childOptionalsOnNonSealed.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: NotSealed.kt
package diag.co

import me.tbsten.cream.ChildOptionals

@ChildOptionals
class NotSealed(val data: String)
