// PATTERN: CRM-T-0417
// TITLE: ParentOptional / invalidUsage / forcedPublicOnInternalParent.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.data" is forced public (via visibility = CopyVisibility.PUBLIC on @ParentOptional) but its signature would expose the internal declaratio
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/forcedPublicOnInternalParent.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.ParentOptional

internal sealed interface MyState {
    data class Success(
        @ParentOptional(visibility = CopyVisibility.PUBLIC) val data: String,
    ) : MyState
    data object Loading : MyState
}
