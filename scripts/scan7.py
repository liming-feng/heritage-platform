# -*- coding: utf-8 -*-
"""列出所有图（drawing）与图题注的先后顺序，用于统一编号"""
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

for m in re.finditer(r'<w:drawing>.*?</w:drawing>|<w:p[ >].*?</w:p>', xml, re.S):
    seg = m.group(0)
    if seg.startswith('<w:drawing'):
        # 抓 embed id 与 docPr name
        name = re.search(r'<wp:docPr[^>]*name="([^"]*)"', seg)
        desc = re.search(r'<wp:docPr[^>]*descr="([^"]*)"', seg)
        emb = re.findall(r'r:embed="([^"]+)"', seg)
        print(f'[IMG] name={name.group(1) if name else "-"} descr={desc.group(1) if desc else "-"} ids={emb}')
    else:
        t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S))
        s = t.strip()
        if s:
            print(f'   P: {s[:90]}')
