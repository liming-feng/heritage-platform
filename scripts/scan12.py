# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix3.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

print('=== 全文所有 "图 x-y" 出现（含引用与题注）===')
for i, s in enumerate(paras):
    t = pt(s)
    for m in re.finditer(r'图\s*\d+\s*[-–]\s*\d+', t):
        ctx = t[max(0, m.start()-30):m.start()+45]
        print(f'p{i}: ...{ctx}...')
