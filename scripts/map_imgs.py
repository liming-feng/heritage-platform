# -*- coding: utf-8 -*-
"""列出文档中每张图片的 media 文件名、题注、以及图片自身尺寸，用于制定替换映射"""
import zipfile, re, os
from PIL import Image
import io

SRC = r"C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"
z = zipfile.ZipFile(SRC)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
rels = z.read('word/_rels/document.xml.rels').decode('utf-8', 'ignore')
relmap = dict(re.findall(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels))

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

print(f'{"序":>3} {"段":>5} {"media":<16} {"尺寸":<14} 题注')
print('-'*88)
k = 0
for i, seg in enumerate(paras):
    if '<w:drawing>' not in seg: continue
    k += 1
    embeds = re.findall(r'r:embed="([^"]+)"', seg)
    m = relmap.get(embeds[0], '?').replace('media/', '') if embeds else '?'
    cap = ''
    for j in range(i+1, min(i+4, len(paras))):
        t = pt(paras[j])
        if re.match(r'^图\s*\d', t): cap = t; break
    tin = pt(seg)
    if not cap and tin: cap = '(段内) ' + tin[:28]
    size = ''
    try:
        im = Image.open(io.BytesIO(z.read('word/media/'+m)))
        size = f'{im.size[0]}x{im.size[1]}'
    except Exception as e:
        size = 'ERR'
    print(f'{k:>3} {i:>5} {m:<16} {size:<14} {cap}')
