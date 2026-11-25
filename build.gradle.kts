// ルートは設定を持たない。共通設定は build-logic の convention plugin（acg.jvm / acg.library / acg.gradle-plugin）
// 例外: docs サイトの API reference をまとめる Dokka（ADR-031）。対象は利用者に公開するモジュールだけ
plugins {
    id("acg.dokka-aggregate")
}

dependencies {
    dokka(project(":processor-api"))
    dokka(project(":gradle-plugin"))
    dokka(project(":testing"))
    dokka(project(":ksp-bridge-api"))
    dokka(project(":ksp-bridge-gradle-plugin"))
}
