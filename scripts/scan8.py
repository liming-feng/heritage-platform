# -*- coding: utf-8 -*-
"""按文档顺序输出 图片占位 与 紧邻题注，用于精确定位编号问题"""
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

seq = []
for m in re.finditer(r'<w:drawing>.*?</w:drawing>|<w:p[ >].*?</w:p>', xml, re.S):
    seg = m.group(0)
    if seg.startswith('<w:drawing'):
        seq.append(('IMG', ''))
    else:
        t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S)).strip()
        seq.append(('P', t))

# 只保留 IMG 与题注，并输出上下文
for i, (k, t) in enumerate(seq):
    if k == 'IMG':
        ctx_before = [x[1][:50] for x in seq[max(0, i-2):i] if x[1]]
        ctx_after = [x[1][:50] for x in seq[i+1:i+4] if x[1]]
        print(f'--- IMG @seq{i} ---')
        print('   前:', ctx_before)
        print('   后:', ctx_after)
