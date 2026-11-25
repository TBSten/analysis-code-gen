// PATTERN: CRM-T-0002
// TITLE: ChildOptionals / invalidUsage / mergedLeafTypeMismatch.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ChildOptionals accessor "diag.co.MyState.value" merges properties with mismatched types: diag.co.MyState.Failure.value: kotlin.Int, diag.co.MyState.Success.value: kotlin.String.
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ChildOptionalsInvalidUsageTest/mergedLeafTypeMismatch.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.co

import me.tbsten.cream.ChildOptionals

@ChildOptionals
sealed interface MyState {
    data class Success(val value: String) : MyState
    data class Failure(val value: Int) : MyState
    data object Loading : MyState
}
