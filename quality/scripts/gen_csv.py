#!/usr/bin/env python3
"""test-case-inventory.md の表から test-cases.csv と件数まとめを作る。"""
import csv, re, sys, collections, pathlib
d = pathlib.Path(__file__).resolve().parent.parent
md = (d / 'test-case-inventory.md').read_text()
rows, area = [], ''
for line in md.splitlines():
    m = re.match(r'### (?:5\.\d+|6\.1) (.+)', line)
    if m: area = m.group(1); continue
    c = [x.strip() for x in line.strip().strip('|').split('|')]
    if len(c) == 5 and re.match(r'^(?:[A-Z]+(-[A-Z]+)*-[A-Z0-9]+[a-z]?|KSPT-[A-Za-z0-9_]+)$', c[0]):
        rows.append(dict(id=c[0], area=area, case=c[1], kind=c[2], milestone=c[3], note=c[4]))
ids = [r['id'] for r in rows]
dup = [i for i, n in collections.Counter(ids).items() if n > 1]
if dup: sys.exit(f'duplicate ids: {dup}')
with open(d / 'test-cases.csv', 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=['id', 'area', 'case', 'kind', 'milestone', 'note'])
    w.writeheader(); w.writerows(rows)
def first_m(r): return r['milestone'][:2]  # "M2 / M3" -> M2, "M1〜M3" -> M1
ms = collections.Counter(first_m(r) for r in rows)
kinds = collections.Counter(r['kind'] for r in rows)
e2e = sum(v for k, v in kinds.items() if k in ('E', 'D', 'P'))
lines = ['| マイルストーン | 新たに通すケース | 累計（合格ライン） |', '|---|---|---|']
acc = 0
for m in ['M1', 'M2', 'M3']:
    acc += ms[m]; lines.append(f'| {m} | {ms[m]} | {acc} |')
lines += ['', f'ほかに「任意」（余力があれば作る。合格ラインに含めない）が {ms["任意"]} 件。']
lines += ['', '| 種別 | 件数 |', '|---|---|'] + [f'| {k} | {kinds[k]} |' for k in 'UIEDPB' if kinds[k]]
lines += ['', f'合計 {len(rows)} 件（うち KSPT {sum(1 for r in rows if r["id"].startswith("KSPT-"))} 件）。E2E 系（E / D / P）は {e2e} 件で全体の {e2e*100//len(rows)}%。', '',
          '備考: BRG-07（KSPT）と RW-ALL-P1 は M が分かれるため、最初の M で数えている。']
s = '\n'.join(lines)
md2 = re.sub(r'<!-- summary:start -->.*<!-- summary:end -->', f'<!-- summary:start -->\n{s}\n<!-- summary:end -->', md, flags=re.S)
(d / 'test-case-inventory.md').write_text(md2)
print(s)
