// PATTERN: CRM-T-0420
// TITLE: ParentOptional / invalidUsage / mergedTypealiasVsExpandedType.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.id" merges properties with mismatched types: diag.po.MyState.Success.id: diag.po.UserId, diag.po.MyState.Failure.id: kotlin.String.
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/mergedTypealiasVsExpandedType.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

typealias UserId = String

sealed interface MyState {
    data class Success(@ParentOptional val id: UserId) : MyState
    data class Failure(@ParentOptional val id: String) : MyState
}
