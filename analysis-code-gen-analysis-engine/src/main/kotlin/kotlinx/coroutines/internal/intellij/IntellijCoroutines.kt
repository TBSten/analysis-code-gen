package kotlinx.coroutines.internal.intellij

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

/**
 * IntelliJ Platform が JetBrains 版の kotlinx-coroutines（`*-intellij-*`）にだけある前提で呼ぶクラスの代わり（ADR-060）。
 *
 * AA を動かすクラスパスには通常の kotlinx-coroutines を載せるため、このクラスが無いと IntelliJ の core / util が
 * NoClassDefFoundError で落ちる。KSP2 は呼び出し側のバイトコードを自前の `IntelliJCoroutinesFacade` へ書き換えて避けている
 * （google/ksp kotlin-analysis-api/src/main/java/com/intellij/util/IntelliJCoroutinesFacade.kt を参考に新規に書いた）。
 * ここでは同じ名前のクラスを置き、同じ振る舞い（並列度の補償をしない）にする。呼ばれるのは次の 3 つだけ（IntelliJ 251.27812.49）。
 */
@Suppress("unused")
public object IntellijCoroutines {
    public fun currentThreadCoroutineContext(): CoroutineContext? = null

    public fun <T> runBlockingWithParallelismCompensation(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> T,
    ): T = runBlocking(context, block)

    public fun softLimitedParallelism(
        dispatcher: CoroutineDispatcher,
        parallelism: Int,
    ): CoroutineDispatcher = dispatcher.limitedParallelism(parallelism)
}
