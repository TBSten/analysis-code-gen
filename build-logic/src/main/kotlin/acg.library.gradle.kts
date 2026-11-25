// 利用者に公開するライブラリモジュール。公開 API は明示（explicitApi）
plugins {
    id("acg.jvm")
    `java-library`
}

kotlin {
    explicitApi()
}
