# -*- coding: utf-8 -*-
"""修正所有图片的显示尺寸(extent)，使其与图片真实像素长宽比一致，
   并处理重复图删除、缺失题注补齐"""
import zipfile, re, os, io
from PIL import Image

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix2.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()

xml = data['word/document.xml'].decode('utf-8')
rels = data['word/_rels/document.xml.rels'].decode('utf-8')
relmap = dict(re.findall(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels))

# 最大可用宽度（页面宽 - 左右边距）：A4 纵向 21cm - 2*3.17cm ≈ 14.66cm
# 1 cm = 360000 EMU
MAX_CX = 5120000   # 约 14.2 cm，留出安全边距
MAX_CY = 6400000   # 约 17.8 cm，避免超高翻页

results = []
changed = 0

def fix_drawing(m):
    """在单个 <w:drawing>...</w:drawing> 内重算 extent"""
    global changed
    seg = m.group(0)
    emb = re.search(r'r:embed="([^"]+)"', seg)
    if not emb:
        return seg
    media = relmap.get(emb.group(1), '').replace('media/', '')
    try:
        im = Image.open(io.BytesIO(data['word/media/' + media]))
        iw, ih = im.size
    except Exception:
        return seg
    if not iw or not ih:
        return seg

    ratio = iw / ih
    cx = MAX_CX
    cy = int(cx / ratio)
    if cy > MAX_CY:          # 太高则按高度约束
        cy = MAX_CY
        cx = int(cy * ratio)

    newext = f'<wp:extent cx="{cx}" cy="{cy}"/>'
    seg2, n = re.subn(r'<wp:extent cx="\d+" cy="\d+"/>', newext, seg)
    # 同步 effectExtent
    seg2, n2 = re.subn(r'<wp:effectExtent l="\d+" t="\d+" r="\d+" b="\d+"/>',
                       '<wp:effectExtent l="0" t="0" r="0" b="0"/>', seg2)
    if n:
        changed += 1
        results.append(f'{media:<16} {iw}x{ih:<7} -> {cx}x{cy}  (ratio {ratio:.3f})')
    return seg2

xml = re.sub(r'<w:drawing>[\s\S]*?</w:drawing>', fix_drawing, xml)

data['word/document.xml'] = xml.encode('utf-8')
if os.path.exists(TMP): os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print(f'=== 修正 {changed} 处图片显示尺寸 ===')
for r in results: print(' ', r)
print('输出:', TMP, os.path.getsize(TMP))
