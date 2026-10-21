# -*- coding: utf-8 -*-
"""修正 v4 —— 合并被拆断的参考文献段落"""
import zipfile, re, os

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix4.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()
xml = data['word/document.xml'].decode('utf-8')
chg = []

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

for i in (826, 827, 828):
    print(f'p{i}: {pt(paras[i])[:120]!r}')

# 定位 p826 与 p828 的原始 XML
def find_para_xml(keyword):
    idx = xml.find(keyword)
    if idx < 0: return None
    b = xml.rfind('<w:p ', 0, idx)
    if b < 0: b = xml.rfind('<w:p>', 0, idx)
    e = xml.find('</w:p>', idx)
    return b, e+6

# 合并：把 p826 尾部 "中国非物质文化遗产网. 国家级" 与 p828 合并成一条完整文献
k1 = '中国非物质文化遗产网'
b1, e1 = find_para_xml(k1)
print('p826 span:', b1, e1)
print(repr(xml[b1:e1]))
