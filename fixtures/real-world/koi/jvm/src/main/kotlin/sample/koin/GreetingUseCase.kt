package sample.koin

import org.koin.core.annotation.Factory

/** 取得するたびに新しく作られる。 */
@Factory
class GreetingUseCase(
    val repository: GreetingRepository,
) {
    operator fun invoke(name: String): String = repository.greeting(name)
}
