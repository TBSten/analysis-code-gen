package sample.inject

import me.tatarka.inject.annotations.Inject

/** 毎回新しく作られる。 */
@Inject
class GreetingUseCase(
    val repository: GreetingRepository,
) {
    operator fun invoke(name: String): String = repository.greeting(name)
}
