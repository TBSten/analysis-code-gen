package com.example

annotation class Hello

@Hello
class Greeter

fun main() {
    println(Greeter().hello()) // Hello, Greeter!
}
