# KSP 公式テスト（KSPT）

理由と範囲は ADR-024。実装者が守ることだけを書く。

- KSPT は `porting/ksp/import_kspt.py` で取り込む。`ksp-compatibility-tests/src/kspt/`・`src/test/resources/kspt/`・一覧 6.1 / 6.2 を手で直さない
- 1 ケース = 上流 `KSPUnitTestSuite` の 1 メソッド。ID は `KSPT-<メソッド名から test を除いたもの>`。API 機能の 4 つの組すべてで期待値どおりなら通過
- Bridge で通ったら `kspt/bridge-enabled.txt` に ID を足す（足すだけ）。testData の期待値は変えない
- Bridge を動かすときは `KsptCases.run` の `KsptEngine.Bridge` の分岐に入れる。testData と Processor は K と同じものを使う
- 期待値は JDK 17 前提。`-Pacg.kspt.jdkHome=<JDK 17>` を渡す（無いと `jdk17Only` のケースが合わない）
- K で全件流す: `./gradlew :ksp-compatibility-tests:test --tests '*KsptKsp2Test*' -Pacg.kspt.k=true`（約 3 分）。1 件だけなら `-Pacg.kspt.only=KSPT-Hello`
- 移植元の KSP を上げたら script を流し直し、除外（`EXCLUDED`）の既知バグが直っていれば外す
