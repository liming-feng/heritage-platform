# -*- coding: utf-8 -*-
"""编号与残留修正 v2 —— 直接操作 document.xml"""
import zipfile, re, os

SRC = r"C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_fix2.docx"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()

xml = data['word/document.xml'].decode('utf-8')
chg = []

def rep(old, new, label, cnt=None):
    global xml
    n = xml.count(old)
    if n == 0:
        chg.append(f'!! 未匹配: {label} ({old[:40]})')
        return 0
    if cnt is not None and n != cnt:
        chg.append(f'!! 数量异常: {label} 期望{cnt} 实际{n}')
    xml = xml.replace(old, new)
    chg.append(f'ok [{n}] {label}')
    return n

# ========== 第三章图号重排 ==========
# 文档顺序：图3-1 部署 / 图3-2 逻辑 / [图3-4 时序] / [图3-3 功能结构] / [图3-4 E-R实体] / [图3-5 E-R联系]
# 目标顺序：图3-1 部署 / 图3-2 逻辑 / 图3-3 功能结构 / 图3-4 时序 / 图3-5 E-R实体 / 图3-6 E-R联系
rep('图3-4 前后端交互时序流程图', '图3-4 前后端交互时序流程图@@KEEP@@', '@@占位')
rep('图3-3 系统功能结构图', '图3-3 系统功能结构图', '功能结构图保持')
rep('图3-4 非遗作品实体 E-R 图', '图3-5 非遗作品实体 E-R 图', 'E-R实体 3-4->3-5')
rep('图3-5 系统实体联系 E-R 图', '图3-6 系统实体联系 E-R 图', 'E-R联系 3-5->3-6')
rep('图3-4 前后端交互时序流程图@@KEEP@@', '图3-4 前后端交互时序流程图', '时序图保持')

# ========== 第五章图号 ==========
rep('图4-4 管理员用户页面', '图5-1 管理员用户页面', '管理员用户页面 4-4->5-1')
rep('图5-14 系统响应时间测试', '图5-2 系统响应时间测试', '响应时间 5-14->5-2')

# ========== 正文引用同步 ==========
rep('便于管理员高效管理用户数据和校园活动如图4-4所示', '便于管理员高效管理用户数据如图5-1所示', '正文引用 4-4->5-1')
rep('响应时间测试的具体结果如图 5-14 所示', '响应时间测试的具体结果如图 5-2 所示', '正文引用 5-14->5-2')

data['word/document.xml'] = xml.encode('utf-8')

if os.path.exists(TMP):
    os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print('=== 修改清单 ===')
for c in chg:
    print(' ', c)
print('输出:', TMP, os.path.getsize(TMP))
