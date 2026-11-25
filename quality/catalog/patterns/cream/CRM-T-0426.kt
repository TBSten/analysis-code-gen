// PATTERN: CRM-T-0426
// TITLE: ParentOptional / invalidUsage / sameChildMergedIntoOneAccessor.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.info" merges multiple properties of the same child diag.po.MyState.Success (message, detail).
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/sameChildMergedIntoOneAccessor.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    data class Success(
        @ParentOptional(propertyName = "info") val message: String,
        @ParentOptional(propertyName = "info") val detail: String,
    ) : MyState
    data object Loading : MyState
}
