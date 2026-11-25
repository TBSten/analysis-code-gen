// PATTERN: CRM-T-0425
// TITLE: ParentOptional / invalidUsage / privateProperty.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional property diag.po.MyState.Success.secret must be public or internal: the generated top-level accessor cannot read a private property.
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/privateProperty.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    class Success(data: String) : MyState {
        @ParentOptional
        private val secret: String = data
    }
    data object Loading : MyState
}
