---
name: port-from-ksp2
description: KSP2（google/ksp）などの上流からコードを移植する手順。analysis-code-gen-ksp-bridge にファイルを写すとき、KSP 公式テストを取り込むとき、「KSP2 から移植」「port」するときに使う。
---

# KSP2 から移植する

出所の書式は `porting/README.md` と `.claude/rules/porting.md`（ADR-018、LIC-01）。

1. **対象を確かめる**: `porting/ksp/port-map.csv` で対象行の `target_module` と `strategy` を見る。移植元は `porting/ksp/UPSTREAM.properties` の固定コミット（KSP 2.3.14）だけ
2. **上流を取得する**（固定コミットで）
   ```bash
   git clone --filter=blob:none https://github.com/google/ksp.git /tmp/ksp
   git -C /tmp/ksp checkout <UPSTREAM.properties の commit>
   ```
3. **ファイルを写す**: パッケージを `me.tbsten.analysis.code.gen.kspbridge...` へ変える。上流のヘッダは 1 文字も変えない。ヘッダが無いファイルは `porting/README.md` の定型を足す
4. **出所の 3 行を足す**: ヘッダの直後に `// Ported-From: google/ksp@<40 桁 SHA>:<パス>` / `// Modified-By: analysis-code-gen contributors, 2026` / `// Modifications: <変更点>`
5. **切り出しが難しい箇所は無理に直さない**: P2 ではほぼそのまま移植し、結合は analysis-code-gen-ksp-bridge の中に閉じ込める（作り込みは P3）
6. **port-map.csv の状態を更新する**
7. **検査を通す**
   ```bash
   python3 scripts/lic01/check_provenance.py
   ./gradlew :analysis-code-gen-ksp-bridge:build --console=plain
   ```
8. **コミット**は `port: <上流ファイル名> を移植 [ACG-n]`。1 コミットに移植（無改変に近い状態）と作り込みを混ぜない（差分を追えるように）
