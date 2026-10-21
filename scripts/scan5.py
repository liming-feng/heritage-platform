# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

tables = re.findall(r'<w:tbl>.*?</w:tbl>', xml, re.S)
print('表格总数:', len(tables))
for i, t in enumerate(tables, 1):
    rows = len(re.findall(r'<w:tr[ >]', t))
    first = re.findall(r'<w:t[^>]*>(.*?)</w:t>', t, re.S)
    head = ' | '.join(first[:5])
    print(f'  表{i}: {rows}行  {head[:80]}')

print()
paras = re.findall(r'<w:p[ >].*?</w:p>', xml, re.S)
def ptext(x): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', x, re.S))
texts = [ptext(x) for x in paras]

print('=== 图题注 ===')
for i, t in enumerate(texts):
    s = t.strip()
    if re.match(r'^图\s*\d+[-–]\d+', s) or re.match(r'^表\s*\d+[-–]\d+', s):
        print(f'  p{i}: {s[:80]}')

print()
print('=== 各级标题（样式识别）===')
for x in paras:
    m = re.search(r'<w:pStyle w:val="([^"]+)"', x)
    t = ptext(x).strip()
    if m and t and m.group(1) in ('2','3','4','000039','1','heading1','heading2','heading3'):
        print(f'  [{m.group(1)}] {t[:70]}')
