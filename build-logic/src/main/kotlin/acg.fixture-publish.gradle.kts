// フィクスチャ（fixtures/projects/*）が読むローカル Maven リポジトリ `build/fixture-repo` へ publish する（ADR-027）。
// Maven Central への公開ではない（公開は Out of Scope）。`./gradlew publishFixtureDeps` でまとめて実行する
plugins {
    `maven-publish`
}

val fixtureRepo = rootProject.layout.buildDirectory.dir("fixture-repo")

publishing {
    repositories {
        maven {
            name = "fixtureRepo"
            url = uri(fixtureRepo)
        }
    }
}

// java-gradle-plugin は自分で publication を作る。それ以外は java コンポーネントから 1 つ作る
pluginManager.withPlugin("java-library") {
    if (!pluginManager.hasPlugin("java-gradle-plugin")) {
        publishing.publications.create<MavenPublication>("fixture") {
            from(components["java"])
            artifactId = project.path.removePrefix(":").replace(':', '-')
        }
    }
}
