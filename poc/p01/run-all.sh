#!/usr/bin/env bash
# P-01 の検証を一括実行する。
#  1. sample を KGP で実コンパイルし、診断を集める（オラクル）: sample/build/oracle/<variant>.txt
#  2. 各 variant の SourceSet 構造をダンプし、AA（analyzer）で解析: sample/build/aa/<variant>/aa-{isolated,whole}.txt
#  3. compare.py で突き合わせ: build/p01-result.md
set -euo pipefail
cd "$(dirname "$0")"
(cd analyzer && ./gradlew -q installDist)
(cd sample && ./oracle.sh)
for v in ok common shared jvm; do
  if [ "$v" = ok ]; then P=""; else P="-Pp01.errors=$v"; fi
  # 上位の metadata 出力（build/classes）は AA 側では使わないので、コンパイルを飛ばす
  (cd sample && ./gradlew -q $P dumpModel -x compileCommonMainKotlinMetadata -x compileJvmAndJsMainKotlinMetadata -x compileWebMainKotlinMetadata >/dev/null)
  mkdir -p sample/build/aa/$v
  cp sample/build/p01-model.json sample/build/aa/$v/model.json
  for mode in isolated whole; do
    analyzer/build/install/p01-analyzer/bin/p01-analyzer sample/build/aa/$v/model.json sample/build/aa/$v $mode > /dev/null
  done
done
mkdir -p build
python3 compare.py sample/build | tee build/p01-result.md
