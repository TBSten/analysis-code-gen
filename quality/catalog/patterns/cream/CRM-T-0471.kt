// PATTERN: CRM-T-0471
// TITLE: SealedCopy / invalidUsage / viaReturnTypeNotAssignable.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: @SealedCopy.Via function 'cloneWith' returns 'String', which is not assignable to 'State.Custom'. Change its return type to 'State.Custom' (or a subtype of it).
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopyInvalidUsageTest/viaReturnTypeNotAssignable.output.md
// LICENSE: Apache-2.0
// STATUS: candidate
// FILE: State.kt
package sealed.diag

import me.tbsten.cream.SealedCopy

@SealedCopy
sealed interface State {
    val id: String

    class Custom(
        override val id: String,
    ) : State {
        @SealedCopy.Via
        fun cloneWith(id: String): String = id
    }
}
