package sample.inject

import me.tatarka.inject.annotations.Inject

/** コンストラクタ注入。AppComponent の中で 1 つだけ作られる（@AppScope）。 */
@AppScope
@Inject
class GreetingRepository(
    private val clock: Clock,
) {
    fun greeting(name: String): String = "Hello, $name at ${clock.now()}"
}
