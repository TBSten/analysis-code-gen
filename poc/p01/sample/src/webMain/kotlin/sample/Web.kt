package sample

/** js + wasmJs の中間（KGP 2.4 の既定テンプレートが作る）。jsMain は jvmAndJsMain と webMain の両方に dependsOn する */
fun webGreeting(): String = greet() + " from web"
