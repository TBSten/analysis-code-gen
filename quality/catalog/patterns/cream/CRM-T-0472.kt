// PATTERN: CRM-T-0472
// TITLE: SealedCopy / invalidUsage / viaUnmappedParameterWithoutDefault.output
// PROCESSORS: cream
// EXPECT: error
// EXPECT_MESSAGE: @SealedCopy.Via parameter 'extra' neither matches an abstract property (by name or @SealedCopy.Map) nor has a default value, so cream cannot supply it. Rename it to an abstract property, add @SealedCo
// TAGS: cream.family=invalidUsage
// ORIGIN: test:TBSten/cream@2162ec8d5261/cream-ksp/src/test/resources/snapshots/SealedCopyInvalidUsageTest/viaUnmappedParameterWithoutDefault.output.md
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
        fun cloneWith(id: String, extra: Int): Custom = Custom(id = id)
    }
}
