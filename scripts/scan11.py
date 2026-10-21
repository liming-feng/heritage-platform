# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)

lines = []
for i, seg in enumerate(paras):
    t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S)).strip()
    has_img = '<w:drawing>' in seg
    if has_img:
        name = re.search(r'<wp:docPr[^>]*name="([^"]*)"', seg)
        lines.append(f'p{i}  [IMG] name={name.group(1) if name else "-"}  text="{t[:40]}"')
    elif t and (re.match(r'^图\s*\d', t) or re.match(r'^表\s*\d', t)):
        lines.append(f'p{i}  [CAP] {t[:70]}')

with open(r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/imgctx.txt", 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))
print('\n'.join(lines))
