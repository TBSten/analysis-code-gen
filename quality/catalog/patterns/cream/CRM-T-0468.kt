// PATTERN: CRM-T-0468
// TITLE: SealedCopy / invalidUsage / multipleViaFunctions.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: Subtype 'State.Custom' declares more than one @SealedCopy.Via function (cloneA, cloneB). Mark exactly one function with @SealedCopy.Via.
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopyInvalidUsageTest/multipleViaFunctions.output.md
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
        fun cloneA(id: String): Custom = Custom(id = id)

        @SealedCopy.Via
        fun cloneB(id: String): Custom = Custom(id = id)
    }
}
