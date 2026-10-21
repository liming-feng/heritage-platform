# -*- coding: utf-8 -*-
import zipfile, re

p = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
z = zipfile.ZipFile(p)
xml = z.read('word/document.xml').decode('utf-8', 'ignore')

# 定位 1.5.1 微信小程序 所在 <w:p> 全段
idx = xml.find('1.5.1 微信小程序')
print('idx =', idx)
before = xml.rfind('<w:p ', 0, idx)
if before == -1: before = xml.rfind('<w:p>', 0, idx)
after = xml.find('</w:p>', idx)
print('--- 段落全文 ---')
print(xml[before:after+6])
