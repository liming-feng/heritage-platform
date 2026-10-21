# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

out = []
for m in re.finditer(r'<w:drawing>[\s\S]*?</w:drawing>|<w:p[ >][\s\S]*?</w:p>', xml):
    seg = m.group(0)
    if seg.startswith('<w:drawing'):
        out.append(('IMG', ''))
    else:
        t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S)).strip()
        out.append(('P', t))

lines = []
for i, (k, t) in enumerate(out):
    if k == 'IMG':
        before = [x[1][:60] for x in out[max(0,i-3):i] if x[1]]
        after  = [x[1][:60] for x in out[i+1:i+4] if x[1]]
        lines.append(f'--- IMG seq#{i} ---')
        lines.append(f'   前: {before}')
        lines.append(f'   后: {after}')

with open(r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/imgctx.txt", 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))
print('written', len(lines))
