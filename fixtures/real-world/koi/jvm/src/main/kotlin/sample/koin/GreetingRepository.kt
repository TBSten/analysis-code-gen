package sample.koin

import org.koin.core.annotation.Single

/** アプリ全体で 1 つ。 */
@Single
class GreetingRepository {
    fun greeting(name: String): String = "Hello, $name"
}
