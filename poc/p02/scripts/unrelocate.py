#!/usr/bin/env python3
"""symbol-processing-aa-embeddable の relocate (ksp. 接頭辞) を元に戻す PoC 用スクリプト。

KSP2 の aa-embeddable は Analysis API / IntelliJ Core を `ksp.` 接頭辞で relocate しているが、
Kotlin メタデータ (d2) は元の名前のまま残っている。そのため Kotlin から型を参照できない。
クラスファイルの定数プール・リソース内の `ksp/<root>/` `ksp.<root>.` を元に戻して整合させる。
PoC 専用。本番では JetBrains の Analysis API アーティファクトを直接使う (ADR-010 参照)。

usage: unrelocate.py <in.jar> <out.jar>
"""
import re
import struct
import sys
import zipfile

ROOTS = [b"_COROUTINE", b"com", b"gnu", b"io", b"it", b"kotlinx", b"net", b"one", b"org"]
_ROOTS = b"(?:" + b"|".join(ROOTS) + b")"
# 1) 型記述子 `Lksp/org/...;`（直前は `(` `;` `<` `[` プリミティブ型の文字など何でもあり得る）
DESC = re.compile(rb"Lksp/(?=" + _ROOTS + rb"/)")
# 2) 先頭またはトークン境界から始まる名前 `ksp.org.jetbrains...` `ksp/com/intellij/...`。
#    `com/google/devtools/ksp/...` のような KSP 自身のパッケージは直前が英字か `/` `.` なので一致しない。
NAME = re.compile(rb"(?:(?<=[^A-Za-z0-9_$./])|^)ksp[./](?=" + _ROOTS + rb"[./])")
# 3) リソースパス `/ksp/org/apache/...`
RES = re.compile(rb"(?:(?<=[^A-Za-z0-9_$.])|^)/ksp/(?=" + _ROOTS + rb"/)")


def fix_bytes(b: bytes) -> bytes:
    if b"ksp" not in b:
        return b
    b = DESC.sub(b"L", b)
    b = RES.sub(b"/", b)
    return NAME.sub(b"", b)


def fix_class(data: bytes) -> bytes:
    out = bytearray(data[:10])
    count = struct.unpack(">H", data[8:10])[0]
    i = 10
    idx = 1
    while idx < count:
        tag = data[i]
        if tag == 1:  # Utf8
            ln = struct.unpack(">H", data[i + 1:i + 3])[0]
            s = data[i + 3:i + 3 + ln]
            ns = fix_bytes(s)
            out += bytes([1]) + struct.pack(">H", len(ns)) + ns
            i += 3 + ln
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            out += data[i:i + 5]; i += 5
        elif tag in (5, 6):
            out += data[i:i + 9]; i += 9; idx += 1
        elif tag in (7, 8, 16, 19, 20):
            out += data[i:i + 3]; i += 3
        elif tag == 15:
            out += data[i:i + 4]; i += 4
        else:
            raise ValueError(f"unknown constant tag {tag} at {i}")
        idx += 1
    out += data[i:]
    return bytes(out)


TEXT_EXT = (".xml", ".properties", ".txt", ".MF", ".version")


def main(src: str, dst: str) -> None:
    seen = set()
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            name = fix_bytes(info.filename.encode()).decode()
            if name.startswith("META-INF/services/"):
                name = "META-INF/services/" + fix_bytes(name[len("META-INF/services/"):].encode()).decode()
            if name in seen:
                continue  # relocate 前後で同名になったものは先勝ち
            seen.add(name)
            data = zin.read(info)
            if name.endswith(".class"):
                data = fix_class(data)
            elif name.startswith("META-INF/services/") or name.endswith(TEXT_EXT):
                data = fix_bytes(data) if b"ksp" in data else data
            zi = zipfile.ZipInfo(name, date_time=(1980, 2, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED
            zout.writestr(zi, data)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
