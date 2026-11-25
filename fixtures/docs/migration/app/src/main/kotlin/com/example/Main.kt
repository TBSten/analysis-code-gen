package com.example

annotation class Hello

@Hello
class Greeter {
    val greeting = "Hi"
}

fun main() {
    println(Greeter().hello())
}
