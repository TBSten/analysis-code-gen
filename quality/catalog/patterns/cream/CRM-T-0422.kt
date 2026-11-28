// PATTERN: CRM-T-0422
// TITLE: ParentOptional / invalidUsage / optionDefaultVisibilityPublicOnInternalParent.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.data" is forced public (via the cream.defaultVisibility=PUBLIC option) but its signature would expose the internal declaration diag.po.My
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/optionDefaultVisibilityPublicOnInternalParent.output.md
// LICENSE: Apache-2.0
// STATUS: rejected
// NOTE: K（KSP2 2.3.14）: K でビルドが通った（EXPECT: error）
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

internal sealed interface MyState {
    data class Success(@ParentOptional val data: String) : MyState
    data object Loading : MyState
}
