// PATTERN: CRM-T-0414
// TITLE: Options / options / invalidDefaultVisibility.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: Invalid option: Invalid ksp.arg["cream.defaultVisibility"] = not-a-visibility.
// TAGS: cream.family=options
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/OptionsDiagnosticTest/invalidDefaultVisibility.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Source.kt
package options.diag

import me.tbsten.cream.CopyTo

@CopyTo(Target::class)
data class Source(val shared: String)

data class Target(val shared: String, val extra: Int)
