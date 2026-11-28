// PATTERN: CRM-T-0424
// TITLE: ParentOptional / invalidUsage / privateChildClass.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Invalid cream usage: @ParentOptional property diag.po.Hidden.secret is declared by a class the generated accessor cannot reference: diag.po.Hidden is private, so the generated `is` check would not com
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/ParentOptionalInvalidUsageTest/privateChildClass.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: MyState.kt
package diag.po

import me.tbsten.cream.ParentOptional

sealed interface MyState {
    data object Loading : MyState
}

private data class Hidden(
    @ParentOptional val secret: String,
) : MyState
