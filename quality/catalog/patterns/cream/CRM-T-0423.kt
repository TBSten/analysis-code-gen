// PATTERN: CRM-T-0423
// TITLE: ParentOptional / invalidUsage / parentAlreadyHasMember.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional cannot generate accessor "status" on diag.po.MyState: a member property with the same name is already visible on diag.po.MyState, and a member always wins over the
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/parentAlreadyHasMember.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    val status: String

    data class Success(
        override val status: String,
        @ParentOptional(propertyName = "status") val code: Int,
    ) : MyState

    data object Loading : MyState {
        override val status: String = ""
    }
}
