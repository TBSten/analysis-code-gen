// PATTERN: CRM-T-0419
// TITLE: ParentOptional / invalidUsage / mergedTypeMismatch.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.value" merges properties with mismatched types: diag.po.MyState.Success.value: kotlin.String, diag.po.MyState.Failure.value: kotlin.Int.
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/mergedTypeMismatch.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    data class Success(@ParentOptional val value: String) : MyState
    data class Failure(@ParentOptional val value: Int) : MyState
    data object Loading : MyState
}
