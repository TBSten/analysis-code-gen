package com.example

annotation class Builder

annotation class Greeting

annotation class Route

@Builder
@Greeting
class Model(val id: String)

@Route
class HomeScreen
