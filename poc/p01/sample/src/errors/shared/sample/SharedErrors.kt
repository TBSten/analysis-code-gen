package sample

// 固有（jvmMain）の API は中間から見えない
fun s1(): String = jvmOnly()

// JVM の API は jvm+js の中間から見えない
fun s2(): String = java.io.File(".").name

// JS の API も見えない
fun s3(): Double = kotlin.js.Date().getTime()

// 別ファイルの private は見えない
fun s4(): Int = commonPrivate()

// 共通の API は見える（エラーにならない）
fun s5(): String = greet() + commonInternal()
