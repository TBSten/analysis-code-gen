#!/usr/bin/env python3
"""P-01: KGP の実コンパイル（オラクル）と AA の解析結果を突き合わせ、Markdown で出す。"""
import os, re, sys
from collections import defaultdict

build = sys.argv[1]  # sample/build
VARIANTS = ["ok", "common", "shared", "jvm"]
# AA の SourceSet → その SourceSet を単独でコンパイルする KGP タスク
TASK = {
    "commonMain": ":compileCommonMainKotlinMetadata",
    "webMain": ":compileWebMainKotlinMetadata",
    "jvmAndJsMain": ":compileJvmAndJsMainKotlinMetadata",
    "jvmMain": ":compileKotlinJvm",
    "jsMain": ":compileKotlinJs",
    "wasmJsMain": ":compileKotlinWasmJs",
}
# 注入したエラーファイルの持ち主
OWNER = {"common": "commonMain", "shared": "jvmAndJsMain", "jvm": "jvmMain"}

# 正常系で必ず出るべき解決結果（手書きの期待値）
EXPECTED = [
    # 中間 SourceSet の actual → 共通の expect
    "A jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:6:12 platformName expects=[sample/platformName@commonMain[Common.kt] expect]",
    # 中間 → 共通の public / internal / expect class
    "R jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:11:27 greet -> sample/greet@commonMain[Common.kt]",
    "R jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:11:37 commonInternal -> sample/commonInternal@commonMain[Common.kt]",
    "R jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:11:56 Clock -> sample/Clock.<init>@commonMain[Common.kt] expect",
    # 中間 → metadata KLIB（coroutines / stdlib の commonMain 部分）
    "R jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:13:45 filter -> kotlinx/coroutines/flow/filter@lib:org.jetbrains.kotlinx-kotlinx-coroutines-core-1.10.2-commonMain",
    "R jvmAndJsMain src/jvmAndJsMain/kotlin/sample/Shared.kt:13:57 toInt -> kotlin/text/toInt@lib:org.jetbrains.kotlin-kotlin-stdlib-2.4.21-commonMain expect",
    "R commonMain src/commonMain/kotlin/sample/Common.kt:21:31 flowOf -> kotlinx/coroutines/flow/flowOf@lib:org.jetbrains.kotlinx-kotlinx-coroutines-core-1.10.2-commonMain",
    # 固有の actual → 共通 / 中間の expect
    "A jvmMain src/jvmMain/kotlin/sample/Jvm.kt:5:14 Clock expects=[sample/Clock@commonMain[Common.kt] expect]",
    "A jvmMain src/jvmMain/kotlin/sample/Jvm.kt:9:12 sharedPlatform expects=[sample/sharedPlatform@jvmAndJsMain[Shared.kt] expect]",
    "R jvmMain src/jvmMain/kotlin/sample/Jvm.kt:11:42 useCommon -> sample/useCommon@jvmAndJsMain[Shared.kt]",
    "A jsMain src/jsMain/kotlin/sample/Js.kt:7:12 sharedPlatform expects=[sample/sharedPlatform@jvmAndJsMain[Shared.kt] expect]",
    # ひし形（jsMain → webMain と jvmAndJsMain の両方）
    "R jsMain src/jsMain/kotlin/sample/Js.kt:9:57 webGreeting -> sample/webGreeting@webMain[Web.kt]",
    "R jsMain src/jsMain/kotlin/sample/Js.kt:9:24 useCommon -> sample/useCommon@jvmAndJsMain[Shared.kt]",
    "A wasmJsMain src/wasmJsMain/kotlin/sample/Wasm.kt:3:12 platformName expects=[sample/platformName@commonMain[Common.kt] expect]",
]

# 既知の差異（合否には入れず記録する）: expect class のメンバーを actual 側から引くと、
# containingModule が actual 側のモジュールになる（PSI は Common.kt のまま）
KNOWN_QUIRKS = [
    "A jvmMain src/jvmMain/kotlin/sample/Jvm.kt:6:16 now expects=[sample/Clock.now@jvmMain[Common.kt] expect]",
]

def oracle(v):
    res = defaultdict(set)
    for line in open(os.path.join(build, "oracle", f"{v}.txt")):
        m = re.match(r"(\S+) e: (\S+?):(\d+):(\d+) (.*)", line.strip())
        if m:
            task, f, l, c, msg = m.groups()
            res[task].add((f, int(l), int(c)))
    return res

def aa(v, mode):
    errs, lines = defaultdict(set), []
    p = os.path.join(build, "aa", v, f"aa-{mode}.txt")
    for line in open(p):
        line = line.rstrip("\n")
        lines.append(line)
        m = re.match(r"E (\S+) (\S+?):(\d+):(\d+) (\S+)", line)
        if m:
            ss, f, l, c, fac = m.groups()
            errs[ss].add((f, int(l), int(c)))
    return errs, lines

out = ["# P-01 突き合わせ結果", ""]
ok_all = True
for mode in ["isolated", "whole"]:
    out += [f"## Session: {mode}", "", "| variant | SourceSet | KGP タスク | オラクル | AA | 一致 |", "|---|---|---|---|---|---|"]
    for v in VARIANTS:
        orc = oracle(v)
        errs, _ = aa(v, mode)
        for ss, task in TASK.items():
            # SourceSet 自身のファイルだけを比べる（固有タスクは下位 SourceSet のファイルも診断するため）
            own = lambda f: f"/{ss}/" in f or (OWNER.get(v) == ss and f.startswith(f"src/errors/{v}/"))
            o = {e for e in orc.get(task, set()) if own(e[0])}
            a = {e for e in errs.get(ss, set()) if own(e[0])}
            same = o == a
            ok_all &= same
            if o or a or v == "ok":
                detail = "" if same else f" 欠落={sorted(o - a)} 余分={sorted(a - o)}"
                out.append(f"| {v} | {ss} | {task} | {len(o)} | {len(a)} | {'✅' if same else '❌'}{detail} |")
    out.append("")

_, lines = aa("ok", "isolated")
missing = [e for e in EXPECTED if e not in lines]
out += ["## 解決結果の期待値（ok, isolated）", "", f"- 期待 {len(EXPECTED)} 件中 一致 {len(EXPECTED) - len(missing)} 件"]
out += [f"  - 不一致: `{m}`" for m in missing]
ok_all &= not missing
quirks = [q for q in KNOWN_QUIRKS if q in lines]
out += ["", "## 既知の差異（合否外）", ""] + [f"- 再現: `{q}`" for q in quirks]

# isolated と whole で解決結果が同じか（param の ID は除く）
def norm(ls): return sorted(l for l in ls if l[:2] in ("R ", "A ", "E ") and "param:" not in l)
for v in VARIANTS:
    same = norm(aa(v, "isolated")[1]) == norm(aa(v, "whole")[1])
    ok_all &= same
    out.append(f"- isolated と whole の解析結果一致（{v}）: {'✅' if same else '❌'}")
out += ["", f"**総合: {'PASS' if ok_all else 'FAIL'}**"]
print("\n".join(out))
sys.exit(0 if ok_all else 1)
