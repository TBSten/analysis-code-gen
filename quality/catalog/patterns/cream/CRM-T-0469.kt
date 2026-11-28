// PATTERN: CRM-T-0469
// TITLE: SealedCopy / invalidUsage / viaErrorReportedOncePerSealedClass.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: @SealedCopy.Via function 'cloneWith' does not supply every abstract property of 'State.Custom'. Missing: count. Add a parameter named after each missing property (or @SealedCopy.Map("<property>")).
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopyInvalidUsageTest/viaErrorReportedOncePerSealedClass.output.md
// LICENSE: Apache-2.0
// STATUS: verified
// FILE: State.kt
package sealed.diag

import me.tbsten.cream.SealedCopy

@SealedCopy
@SealedCopy(funName = "copyOrNull")
sealed interface State {
    val id: String
    val count: Int

    class Custom(
        override val id: String,
        override val count: Int,
    ) : State {
        @SealedCopy.Via
        fun cloneWith(id: String): Custom = Custom(id = id, count = this.count)
    }
}
