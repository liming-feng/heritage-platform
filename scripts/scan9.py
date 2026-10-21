# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

pat = re.compile(r'<w:drawing>|<w:p[ >]|</w:p>', re.S)
seq = []
for m in re.finditer(r'<w:drawing>[\s\S]*?</w:drawing>|<w:p[ >][\s\S]*?</w:p>', xml):
    seg = m.group(0)
    if seg.startswith('<w:drawing'):
        seq.append(('IMG', ''))
    else:
        t = ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', seg, re.S)).strip()
        seq.append(('P', t))

for i, (k, t) in enumerate(seq):
    if k == 'IMG':
        before = [x[1][:60] for x in seq[max(0,i-3):i] if x[1]]
        after  = [x[1][:60] for x in seq[i+1:i+4] if x[1]]
        print(f'--- IMG seq#{i} ---')
        print('   前:', before)
        print('   后:', after)
