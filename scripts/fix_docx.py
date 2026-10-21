# -*- coding: utf-8 -*-
"""直接对交付 docx 做 ZIP/XML 级修正"""
import zipfile, re, shutil, os, sys

SRC = r"C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()

xml = data['word/document.xml'].decode('utf-8')

changes = []

# ---- 修正 1：目录残留条目 "1.5.1 微信小程序" ----
old = '>1.5.1 微信小程序<'
new = '>1.5.1 前端技术<'
if old in xml:
    xml = xml.replace(old, new)
    changes.append(f'TOC条目 1.5.1 微信小程序 -> 1.5.1 前端技术')

# ---- 修正 2：正文小标题 "1.5.1 微信小程序"（若有独立正文标题）----
if '1.5.1 微信小程序' in xml:
    n = xml.count('1.5.1 微信小程序')
    xml = xml.replace('1.5.1 微信小程序', '1.5.1 前端技术')
    changes.append(f'残余 1.5.1 微信小程序 x{n} -> 1.5.1 前端技术')

# ---- 修正 3：结论 p803 段 "用户管理、****、数据统在关键技术实现方面" ----
old2 = '用户管理、****、数据统在关键技术实现方面'
new2 = '用户管理、匠人审核、商品与课程审核、预约监管与数据统计等功能。在关键技术实现方面'
if old2 in xml:
    xml = xml.replace(old2, new2)
    changes.append('结论段 **** 碎片修复')
else:
    print('!! 未匹配 **** 碎片，尝试模糊匹配')
    m = re.search(r'用户管理、(.*?)在关键技术实现方面', xml)
    if m:
        print('   实际片段:', repr(m.group(0)[:80]))

# ---- 修正 4：去掉段落末尾多余的 "稳定运行。"（重复收尾）----
old3 = '并缓解缓存穿透与雪崩风险。稳定运行。'
new3 = '并缓解缓存穿透与雪崩风险，保障了系统在高并发场景下的稳定运行。'
if old3 in xml:
    xml = xml.replace(old3, new3)
    changes.append('结论收尾语句修复')

data['word/document.xml'] = xml.encode('utf-8')

# 重新打包（保持压缩）
if os.path.exists(TMP):
    os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print('=== 修改清单 ===')
for c in changes:
    print(' ', c)
print('输出:', TMP, os.path.getsize(TMP), 'bytes')
