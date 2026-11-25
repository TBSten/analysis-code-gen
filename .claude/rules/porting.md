---
paths:
  - "analysis-code-gen-ksp-bridge/**"
  - "analysis-code-gen-ksp-bridge-api/**"
  - "ksp-compatibility-tests/**"
  - "porting/**"
  - "NOTICE"
---

# 移植コードの出所記録（ADR-018・LIC-01）

KSP2 など上流から写したコードは、Apache License 2.0 §4 を満たすよう出所を残す。書式の正は `porting/README.md`。手順は skill `port-from-ksp2`。

- 上流のヘッダは 1 文字も変えずに残し、直後に `// Ported-From:` `// Modified-By:` `// Modifications:` の 3 行を足す
- `Ported-From` は `<owner>/<repo>@<40 桁 SHA>:<上流パス>`。タグ・短縮 SHA は不可。SHA は `porting/<id>/UPSTREAM.properties` の固定値を使う
- 改変を重ねたら `Modifications` に追記する
- `porting/<id>/port-map.csv` の該当行の状態を更新する
- 移植元のバージョンは固定（KSP 2.3.14）。上げるのは ADR を書いてから
- 切り出しが難しい箇所は P2 ではほぼそのまま移植し、結合は bridge の中に閉じ込める。作り込みは P3
- push 前に `python3 scripts/lic01/check_provenance.py` を通す
