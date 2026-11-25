// PATTERN: CRM-T-0427
// TITLE: ParentOptional / invalidUsage / typeParameterNotPinnedByParent.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional property diag.po.MyState.Success.data references type parameter(s) T that are not pinned by the sealed parent diag.po.MyState, so its type cannot be expressed on t
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/typeParameterNotPinnedByParent.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    data class Success<T>(@ParentOptional val data: T) : MyState
    data object Loading : MyState
}
