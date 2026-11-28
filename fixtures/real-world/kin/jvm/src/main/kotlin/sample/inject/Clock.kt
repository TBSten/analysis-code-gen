package sample.inject

/** 時刻の取得元。AppComponent の @Provides で実装を渡す。 */
fun interface Clock {
    fun now(): Long
}
