// ルートで公開モジュールの Dokka をまとめる（ADR-031）。./gradlew dokkaGenerate の build/dokka/html を
// docs ワークフロー（ADR-026）がサイトの /api/ に置く
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    moduleName.set("analysis-code-gen")
}
