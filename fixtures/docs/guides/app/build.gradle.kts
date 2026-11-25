plugins {
    kotlin("jvm") version "2.4.21"
    id("me.tbsten.analysiscodegen") version "0.1.0"
    application
}

dependencies {
    analysisCodeGen(project(":greeting-processor"))
    analysisCodeGen(project(":builder-processor"))
    analysisCodeGen(project(":registry-processor"))
    analysisCodeGen(project(":deferred-processor"))
}

analysisCodeGen {
    // すべての実行単位に渡すオプション
    arg("greeting.prefix", "Hi")
    // ラウンド数の上限（既定 100）
    maxRounds = 10
    // 最後まで解決できなかった Deferred を警告にする（既定はエラー）
    unresolvedDeferred = DeferredSeverity.Warning
    worker {
        // 別プロセスで動かすと、ヒープを分けられる
        isolation = WorkerIsolation.Process
        maxHeapSize = "2g"
    }
}

application { mainClass = "com.example.MainKt" }
