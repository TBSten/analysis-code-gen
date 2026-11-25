plugins {
    kotlin("jvm") version "2.4.21"
}

dependencies {
    compileOnly("me.tbsten.analysiscodegen:processor-api:0.1.0")
    // テストでは processor-api と実行時の Analysis API も testing が持ってくる
    testImplementation("me.tbsten.analysiscodegen:testing:0.1.0")
    testImplementation(kotlin("test"))
    // gradleFixture を使うときだけ要る
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
    // HelloGradleTest がこの Processor の jar を使う
    dependsOn(tasks.jar)
    systemProperty("hello.processorJar", tasks.jar.get().archiveFile.get().asFile.path)
}
