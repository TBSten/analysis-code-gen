plugins {
    id("acg.library")
    id("acg.serialization")
    id("acg.fixture-publish")
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
