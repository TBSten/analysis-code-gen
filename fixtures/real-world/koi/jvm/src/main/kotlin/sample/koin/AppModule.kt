package sample.koin

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** パッケージ sample.koin の @Single・@Factory を集める。Koin が `AppModule().module` を作る。 */
@Module
@ComponentScan("sample.koin")
class AppModule
