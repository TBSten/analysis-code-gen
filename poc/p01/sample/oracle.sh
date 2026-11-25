#!/usr/bin/env bash
# KGP の実コンパイルで診断を集める（オラクル）。出力: build/oracle/<variant>.txt
set -u
cd "$(dirname "$0")"
mkdir -p build/oracle
TASKS="compileKotlinMetadata compileJvmAndJsMainKotlinMetadata compileWebMainKotlinMetadata compileKotlinJvm compileKotlinJs compileKotlinWasmJs"
for v in ok common shared jvm; do
  if [ "$v" = ok ]; then P=""; else P="-Pp01.errors=$v"; fi
  ./gradlew --continue $P $TASKS --rerun-tasks 2>&1 \
    | awk '/^> Task :/ {t=$3} /^e: file:\/\// {print t" "$0}' \
    | sed "s#file://$(pwd)/##" | sort -u > build/oracle/$v.txt
  echo "== $v: $(wc -l < build/oracle/$v.txt) errors"
done
