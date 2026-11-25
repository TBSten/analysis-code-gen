// すべての JVM モジュールに共通の設定（Kotlin/JVM・JVM 17 ターゲット・kotest・ktlint）
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "me.tbsten.analysiscodegen"
version = providers.gradleProperty("acg.version").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(providers.gradleProperty("acg.warningsAsErrors").map { it.toBoolean() }.orElse(false))
    }
}

dependencies {
    "testImplementation"(libs.findLibrary("kotest-runner-junit5").get())
    "testImplementation"(libs.findLibrary("kotest-assertions-core").get())
    "testImplementation"(libs.findLibrary("kotest-property").get())
}

// CI から渡す値（ADR-026）。-P で受けて、テスト JVM の system property に同じ名前で渡す
//   acg.kotlin      テスト対象の Kotlin 版。未指定なら gradle/kotlin-versions.txt の最新 stable（ADR-011）
//   acg.runPending  true で保留中（@PendingCase）のケースも実行し、予想外の通過を記録する（夜間）
val kotlinVersionsFile = rootProject.layout.projectDirectory.file("gradle/kotlin-versions.txt")
val latestStableKotlin: Provider<String> =
    providers.fileContents(kotlinVersionsFile).asText.map { text ->
        text.lineSequence()
            .map { it.substringBefore('#').trim().split(Regex("\\s+")) }
            .filter { it.size >= 2 && (it[1] == "baseline" || it[1] == "stable") }
            .last()[0]
    }
val acgKotlin: Provider<String> = providers.gradleProperty("acg.kotlin").orElse(latestStableKotlin)
val acgRunPending: Provider<String> = providers.gradleProperty("acg.runPending").orElse("false")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("acg.kotlin", acgKotlin.get())
    systemProperty("acg.runPending", acgRunPending.get())
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
