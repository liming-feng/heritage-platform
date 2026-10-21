# -*- coding: utf-8 -*-
"""拆分 p531：图片与题注同段 → 分成两段"""
import zipfile, re, os

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix3.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix4.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()
xml = data['word/document.xml'].decode('utf-8')

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

# 找含 image22 图片的段落
target = None
for p in paras:
    if 'image22' in p or ('<w:drawing>' in p and '图5-1' in pt(p)):
        target = p; break
assert target, '未找到目标段落'
print('原文段落文字:', pt(target)[:60])
print('长度:', len(target))

def get_ppr(p):
    m = re.search(r'<w:pPr>[\s\S]*?</w:pPr>', p)
    return m.group(0) if m else ''

ppr = get_ppr(target)
# 提取 drawing run
m = re.search(r'<w:r>[\s\S]*?<w:drawing>[\s\S]*?</w:drawing>[\s\S]*?</w:r>', target)
assert m, '未提取到 drawing run'
drawing_run = m.group(0)

# 提取题注文字
caption = ''
for mm in re.finditer(r'<w:t[^>]*>([^<]*)</w:t>', target):
    if '图' in mm.group(1) and re.search(r'图\s*5', mm.group(1)):
        caption = mm.group(1); break
assert caption, '未提取到题注'
print('题注:', caption)

new = (f'<w:p>{ppr}{drawing_run}</w:p>'
       f'<w:p>{ppr}<w:r><w:rPr><w:rFonts w:hint="eastAsia"/></w:rPr><w:t>{caption}</w:t></w:r></w:p>')
xml = xml.replace(target, new, 1)

data['word/document.xml'] = xml.encode('utf-8')
if os.path.exists(TMP): os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()
print('输出:', TMP, os.path.getsize(TMP))
