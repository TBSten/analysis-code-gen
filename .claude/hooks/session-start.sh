#!/bin/bash
# クラウド環境（Claude Code on the web）で JDK・Gradle・依存を使える状態にする（ADR-029）
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}"

# JDK 21 が要る（生成物は JVM 17 向け、ADR-025）
if [ -z "${JAVA_HOME:-}" ] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "21'; then
  for jdk in /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-21-openjdk-arm64 /usr/lib/jvm/openjdk-21; do
    if [ -x "$jdk/bin/java" ]; then
      export JAVA_HOME="$jdk"
      break
    fi
  done
fi
if [ -z "${JAVA_HOME:-}" ]; then
  if ! command -v apt-get >/dev/null; then
    echo "JDK 21 が見つからない" >&2
    exit 1
  fi
  apt-get update -qq
  apt-get install -y -qq openjdk-21-jdk-headless
  export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
fi
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export JAVA_HOME=\"$JAVA_HOME\"" >> "$CLAUDE_ENV_FILE"
fi

# Maven Central の 429 を避けるミラー（ADR-025）
mkdir -p "$HOME/.gradle/init.d"
cp .claude/hooks/maven-central-mirror.init.gradle "$HOME/.gradle/init.d/maven-central-mirror.init.gradle"

# Gradle 本体と依存を取得してコンパイルまで済ませる。ビルド定義が変わったときだけ走らせる
hashes="$HOME/.gradle/.acg-session-start.sha256"
if ! sha256sum --check --status "$hashes" 2>/dev/null; then
  rm -f "$hashes"
  ./gradlew assemble testClasses --console=plain --quiet
  find . -path ./.git -prune -o -path '*/build' -prune -o \
    \( -name '*.gradle.kts' -o -name 'libs.versions.toml' -o -name 'gradle-wrapper.properties' \) -print \
    | sort \
    | xargs sha256sum .claude/hooks/session-start.sh .claude/hooks/maven-central-mirror.init.gradle > "$hashes" \
    || rm -f "$hashes"
fi
