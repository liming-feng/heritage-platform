# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

print('=== 封面信息 ===')
for i in range(0, 20):
    t = pt(paras[i])
    if t: print(f'  p{i}: {t}')

print()
print('=== 参考文献 ===')
n = 0
for i, s in enumerate(paras):
    t = pt(s)
    m = re.match(r'^\[(\d+)\]', t)
    if m:
        n += 1
        print(f'  [{m.group(1)}] {t[:100]}')
print('  参考文献条数:', n)

print()
print('=== 致谢 ===')
started = False
for i, s in enumerate(paras):
    t = pt(s)
    if t == '致谢' or t == '致　谢' or t.startswith('致谢'):
        started = True
        print(f'  p{i} [致谢标题]')
        continue
    if started and t:
        print(f'  p{i}: {t[:80]}')
        if len(t) > 60: pass

print()
print('=== 表格 ===')
tbls = re.findall(r'<w:tbl>[\s\S]*?</w:tbl>', xml)
for i, t in enumerate(tbls, 1):
    rows = len(re.findall(r'<w:tr[ >]', t))
    cells = re.findall(r'<w:tc>[\s\S]*?</w:tc>', t)
    print(f'  表{i}: {rows}行')
