package com.example

annotation class Greeting

annotation class Builder

@Greeting
class Greeter

@Builder
class User(val name: String, val age: Int)

fun main() {
    println(Greeter().greet()) // Hi, Greeter!
    println(UserBuilder().name("Alice").age(20).build().name) // Alice
}
