package sample

// 兄弟（jsMain）の API は見えない
fun j1(): String = jsOnly()

// 型の不一致
fun j2(): Int = sharedPlatform()

// 共通・中間の API は見える
fun j3(): String = useCommon() + greet() + Clock().now()
