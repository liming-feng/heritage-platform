# -*- coding: utf-8 -*-
"""提取文档中每张图片与其题注的配对关系，并把图片导出到磁盘供人工核查"""
import zipfile, re, os

SRC = r"C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"
OUT = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/imgs"
os.makedirs(OUT, exist_ok=True)

z = zipfile.ZipFile(SRC)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
rels = z.read('word/_rels/document.xml.rels').decode('utf-8', 'ignore')

# rId -> media path
relmap = dict(re.findall(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels))

paras = re.findall(r'<w:p[ >][\s\S]*?</w:p>', xml)
def pt(s): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', s, re.S)).strip()

print('=== 图片-题注配对 ===')
img_seq = 0
for i, seg in enumerate(paras):
    if '<w:drawing>' not in seg:
        continue
    img_seq += 1
    embeds = re.findall(r'r:embed="([^"]+)"', seg)
    media = [relmap.get(e, '?').replace('media/', '') for e in embeds]
    # 往前 3 段找题注
    cap = ''
    for j in range(i+1, min(i+4, len(paras))):
        t = pt(paras[j])
        if re.match(r'^图\s*\d', t):
            cap = t; break
    txt_in = pt(seg)
    print(f'#{img_seq} p{i} media={media} 题注="{cap}" 段内文字="{txt_in[:40]}"')

    for e, m in zip(embeds, media):
        try:
            data = z.read('word/media/' + m)
            ext = os.path.splitext(m)[1] or '.png'
            with open(os.path.join(OUT, f'{img_seq:02d}_p{i}{ext}'), 'wb') as f:
                f.write(data)
        except Exception as ex:
            print('   导出失败', m, ex)

print()
print('导出目录:', OUT)
print(os.listdir(OUT))
