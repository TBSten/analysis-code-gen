package com.example

import com.squareup.moshi.JsonClass

annotation class Greeting

annotation class Describe

@JsonClass(generateAdapter = true)
@Greeting
@Describe
data class User(val name: String, val age: Int) {
    fun isAdult(): Boolean = age >= 20
}
