// PATTERN: CRM-T-0416
// TITLE: ParentOptional / invalidUsage / forcedPublicExposesInternalPropertyType.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional accessor "diag.po.MyState.d" is forced public (via visibility = CopyVisibility.PUBLIC on @ParentOptional) but its signature would expose the internal declaration d
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/forcedPublicExposesInternalPropertyType.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: Detail.kt
package diag.po

import me.tbsten.cream.CopyVisibility
import me.tbsten.cream.ParentOptional

internal class Detail(val x: Int)

sealed interface MyState {
    data object Loading : MyState
}

internal data class Hidden(
    @ParentOptional(visibility = CopyVisibility.PUBLIC) val d: Detail,
) : MyState
