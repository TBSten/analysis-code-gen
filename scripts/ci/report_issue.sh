#!/usr/bin/env bash
# 定期実行の結果を GitHub issue で知らせる（ADR-026）。同じラベルの open な issue が 1 つだけになるようにする
#   report_issue.sh open  <label> <title> <body-file>   失敗・検知: 既存 issue にコメント、無ければ作る
#   report_issue.sh close <label> <message>             回復: open な issue にコメントして閉じる
# 必要: gh、GH_TOKEN（issues: write）、GITHUB_REPOSITORY
set -euo pipefail
mode=$1 label=$2
gh label create "$label" --repo "$GITHUB_REPOSITORY" --color B60205 --force >/dev/null
existing=$(gh issue list --repo "$GITHUB_REPOSITORY" --label "$label" --state open --json number --jq '.[0].number // empty')
case "$mode" in
  open)
    title=$3 body=$4
    if [[ -n "$existing" ]]; then
      gh issue comment "$existing" --repo "$GITHUB_REPOSITORY" --body-file "$body"
    else
      gh issue create --repo "$GITHUB_REPOSITORY" --label "$label" --title "$title" --body-file "$body"
    fi
    ;;
  close)
    if [[ -n "$existing" ]]; then
      gh issue close "$existing" --repo "$GITHUB_REPOSITORY" --comment "$3"
    fi
    ;;
  *) echo "usage: $0 open|close ..." >&2; exit 2 ;;
esac
