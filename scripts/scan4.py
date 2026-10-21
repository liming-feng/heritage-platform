# -*- coding: utf-8 -*-
import zipfile, re
p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')
paras = re.findall(r'<w:p[ >].*?</w:p>', xml, re.S)
def ptext(x): return ''.join(re.findall(r'<w:t[^>]*>(.*?)</w:t>', x, re.S))
texts = [ptext(x) for x in paras]
print('--- p230 全文 ---')
print(texts[230])
print()
print('含 ** 的位置:')
t = texts[230]
for m in re.finditer(r'\*\*', t):
    print('   offset', m.start(), '|', repr(t[max(0,m.start()-60):m.start()+60]))
