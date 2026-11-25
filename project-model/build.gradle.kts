plugins {
    id("acg.library")
    id("acg.serialization")
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
