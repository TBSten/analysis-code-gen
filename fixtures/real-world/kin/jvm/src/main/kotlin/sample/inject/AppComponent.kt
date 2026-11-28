package sample.inject

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/** kotlin-inject が InjectAppComponent と `AppComponent::class.create(...)` を作る。 */
@AppScope
@Component
abstract class AppComponent(
    private val fixedTime: Long,
) {
    abstract val useCase: GreetingUseCase

    abstract val repository: GreetingRepository

    @Provides
    fun clock(): Clock = Clock { fixedTime }
}
