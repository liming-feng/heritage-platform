# -*- coding: utf-8 -*-
import zipfile, re, os

p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

# 清洗：去掉 pPr/rPr 等属性区，只保留 w:t 与段落边界；用于定位可疑词
paras = re.findall(r'<w:p[ >].*?</w:p>', xml, re.S)
def ptext(x):
    return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', x, re.S))
texts = [ptext(x) for x in paras]

print('--- p70 原文 ---')
print(texts[70][:400])
print()
print('--- p214 原文 ---')
print(texts[214][:600])
print()
print('--- p230 原文 ---')
print(texts[230][:600])
print()
print('--- p803 原文 ---')
print(texts[803])
print()
print('--- p544 ---'); print(texts[544][:300])
print('--- p559 ---'); print(texts[559][:300])
print('--- p673 ---'); print(texts[673])
