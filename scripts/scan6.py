# -*- coding: utf-8 -*-
"""列出所有图（drawing）与图题注的先后顺序，用于统一编号"""
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

# 按文档顺序切分为 token 序列
body = xml
tokens = []
for m in re.finditer(r'<w:drawing>.*?</w:drawing>|<w:p[ >].*?</w:p>', body, re.S):
    seg = m.group(0)
    if seg.startswith('<w:drawing'):
        tokens.append(('IMG', seg))
    else:
        t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S))
        tokens.append(('P', t))

print('=== 文档顺序中的图片与题注 ===')
n = 0
for kind, seg in tokens:
    if kind == 'IMG':
        n += 1
        # 尝试取图片描述/文件名
        print(f'  [IMG #{n}]  <图片>')
    else:
        s = seg.strip()
        if s and (re.match(r'^图\s*\d+[-–]\d+', s) or re.match(r'^表\s*\d+[-–]\d+', s)):
            print(f'        {s[:80]}')
