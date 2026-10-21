# -*- coding: utf-8 -*-
"""回填所有修正后的图片 + 修正题注"""
import zipfile, re, os, shutil

SRC = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/deliverable.docx"
TMP = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/_verify/_imgfix.docx"
FIG = r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/report_figs"

zin = zipfile.ZipFile(SRC, 'r')
items = zin.infolist()
data = {i.filename: zin.read(i.filename) for i in items}
zin.close()

def readfig(name):
    with open(os.path.join(FIG, name), 'rb') as f:
        return f.read()

# ---------- 1. 替换 media 图片内容 ----------
img_map = {
    'word/media/image1.png':  'fig2-1_user_uc.png',        # 图2-1 普通用户用例图
    'word/media/image2.png':  'fig2-2_craftsman_uc.png',   # 图2-2 匠人用例图
    'word/media/image3.png':  'fig2-3_admin_uc.png',       # 图2-3 管理员用例图
    'word/media/image11.png': 'fig3-5_goods_er.png',       # 图3-5 非遗作品实体 E-R 图
    'word/media/image18.png': 'fig4-5_booking_audit_seq.png', # 图4-5 预约审核与核销顺序图
    'word/media/image19.png': 'fig4-5_booking_activity.png',  # 图4-6 预约业务流程活动图
    'word/media/image22.png': 'fig5-1_admin_user.png',     # 图5-1 管理员用户页面
    'word/media/image23.png': 'fig5-2_perf.png',           # 图5-2 响应时间测试
}
done = []
for target, src in img_map.items():
    if target not in data:
        done.append(f'!! 缺少 {target}')
        continue
    data[target] = readfig(src)
    done.append(f'ok {target} <- {src}')

# ---------- 2. 修正题注 ----------
xml = data['word/document.xml'].decode('utf-8')
cap = []
def rep(old, new, label):
    global xml
    n = xml.count(old)
    if n == 0:
        cap.append(f'!! 未匹配: {label}')
        return
    xml = xml.replace(old, new)
    cap.append(f'ok [{n}] {label}')

data['word/document.xml'] = xml.encode('utf-8')

# ---------- 3. 重新打包 ----------
if os.path.exists(TMP): os.remove(TMP)
zout = zipfile.ZipFile(TMP, 'w', zipfile.ZIP_DEFLATED)
for i in items:
    zout.writestr(i, data[i.filename])
zout.close()

print('=== 图片替换 ===')
for d in done: print(' ', d)
print('=== 题注修改 ===')
for c in cap: print(' ', c)
print('输出:', TMP, os.path.getsize(TMP))
