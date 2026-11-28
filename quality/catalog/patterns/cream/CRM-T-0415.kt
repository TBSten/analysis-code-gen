// PATTERN: CRM-T-0415
// TITLE: ParentOptional / invalidUsage / extensionProperty.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional cannot be applied to extension property diag.po.MyState.Success.ext: the generated accessor reads the property on the child instance alone and cannot supply the ex
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/extensionProperty.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    class Success : MyState {
        @ParentOptional
        val String.ext: Int get() = length
    }
    data object Loading : MyState
}
