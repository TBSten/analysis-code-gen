// 公開モジュールの API reference（Dokka）。docs サイトの /api-docs/ に載る（ADR-031）
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/TBSten/analysis-code-gen/tree/main/${project.name}/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}
