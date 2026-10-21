# -*- coding: utf-8 -*-
"""结构修正：删除重复图、拆分混排段落、调整题注位置"""
import zipfile, re, os

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix2.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix3.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()
xml = data['word/document.xml'].decode('utf-8')
log = []

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

# 定位关键段落的原始 XML
def para_xml_of(idx):
    """按出现顺序取第 idx 个段落"""
    return paras[idx]

# ---------- A. 删除 p327（重复的逻辑架构图，含 image5） ----------
p327 = paras[327]
assert 'image5' in p327 or '<w:drawing>' in p327, 'p327 不含图片!'
xml = xml.replace(p327, '', 1)
log.append(f'删除 p327 重复图（{len(p327)} 字符）')

# ---------- B. p340 题注 + p341 图/正文 混排 → 重组 ----------
# 目标顺序: [正文] [图] [题注]
# p340 现为纯题注段落；p341 为 图+正文混排
p340 = paras[340]
p341 = paras[341]

# 提取 p341 的 drawing 部分
m = re.search(r'(<w:r>\s*<w:rPr>[\s\S]*?</w:rPr>\s*<w:drawing>[\s\S]*?</w:drawing>\s*</w:r>)', p341)
if not m:
    m = re.search(r'(<w:r>[\s\S]*?<w:drawing>[\s\S]*?</w:drawing>[\s\S]*?</w:r>)', p341)
drawing_run = m.group(1)
# 提取 p341 的正文文字
body_text = pt(p341)
# 去掉段落里原有的文字 run：保留包含文字的部分
# 构造新段：正文段 + 图片段（复用 p341 的 pPr）+ 题注段（复用 p340 的 pPr）
def get_ppr(p):
    mm = re.search(r'<w:pPr>[\s\S]*?</w:pPr>', p)
    return mm.group(0) if mm else ''

p341_ppr = get_ppr(p341)
p340_ppr = get_ppr(p340)

para_text = f'<w:p>{p341_ppr}<w:r><w:t xml:space="preserve">{body_text}</w:t></w:r></w:p>'
para_img  = f'<w:p>{p341_ppr}{drawing_run}</w:p>'
para_cap  = f'<w:p>{p340_ppr}<w:r><w:t>图3-4 前后端交互时序流程图</w:t></w:r></w:p>'

# 用三段替换原 p340+p341
# 注意：p340 在 xml 中位于 p341 之前
# 先替换 p340 为 正文段，再替换 p341 为 图段+题注段
xml = xml.replace(p340, para_text, 1)
xml = xml.replace(p341, para_img + para_cap, 1)
log.append('重组 p340/p341：[正文] [图3-4] [题注3-4]')

data['word/document.xml'] = xml.encode('utf-8')
if os.path.exists(TMP): os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print('=== 结构修正 ===')
for l in log: print(' ', l)
print('输出:', TMP, os.path.getsize(TMP))
