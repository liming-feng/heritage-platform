# -*- coding: utf-8 -*-
"""修正 v4 —— 修复被拆断的参考文献条目"""
import zipfile, re, os

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix4.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()
xml = data['word/document.xml'].decode('utf-8')
chg = []

FULL = '中国非物质文化遗产网. 国家级非物质文化遗产代表性项目名录[EB/OL]. (2024-08-10). https://www.ihchina.cn/project.'

# 步骤1: 把 p826 的文本替换为完整文献
old1 = '<w:t>中国非物质文化遗产网. 国家级</w:t>'
new1 = f'<w:t>{FULL}</w:t>'
if old1 in xml:
    xml = xml.replace(old1, new1)
    chg.append('ok p826 补齐为完整文献')

# 步骤2: 删除 p828（原残余片段）整段，只留空段以保持编号列表
old2 = '<w:t>非物质文化遗产代表性项目名录[EB/OL]. (2024-08-10). https://www.ihchina.cn/project.</w:t>'
if old2 in xml:
    xml = xml.replace(old2, '<w:t></w:t>')
    chg.append('ok p828 残余片段已清空')

data['word/document.xml'] = xml.encode('utf-8')
if os.path.exists(TMP):
    os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print('=== 修改清单 ===')
for c in chg: print(' ', c)
print('输出:', TMP, os.path.getsize(TMP))
