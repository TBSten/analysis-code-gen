// PATTERN: CRM-T-0421
// TITLE: ParentOptional / invalidUsage / noSealedParent.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional property diag.po.Plain.data has no sealed parent type: its enclosing class does not (transitively) implement any sealed class/interface, so there is no receiver to
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/noSealedParent.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: Plain.kt
package diag.po

import me.tbsten.cream.ParentOptional

class Plain(@ParentOptional val data: String)
