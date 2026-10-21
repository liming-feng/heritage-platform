# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix2.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

for i in [341, 344, 356, 359, 516, 519, 529, 537, 544, 546, 549, 557, 559]:
    print(f'===== p{i} =====')
    print(pt(paras[i]))
    print()
