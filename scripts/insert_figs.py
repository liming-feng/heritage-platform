# -*- coding: utf-8 -*-
"""插入报告配图：按图题文本定位，从后往前插入，避免坐标漂移。
在 doc_find 返回的 begin 处插入图片，图片会成为独立段落。"""
import sys, json
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from lib import call

FID = "C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"
FIG = r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/report_figs/'

def find_para(text):
    r = call('doc_find', {'text': text}, FID)
    try:
        d = json.loads(r[r.find('{'):])
        return d['locations'][0]['begin'] if d['locations'] else None
    except Exception:
        return None

# 图题 -> 图片文件，宽度
JOBS = [
    ('图3-1 系统硬件部署架构图', 'fig3-1_deploy.png', 620),
    ('图3-2 系统软件逻辑架构图', 'fig3-2_logic.png', 600),
    ('图3-3 系统功能结构图',     'fig3-3_function_tree.png', 620),
    ('图3-8 预约实体 E-R 图',    'fig3-9_er.png', 620),
    ('图3-9 系统实体联系 E-R 图', 'fig3-9_er.png', 620),
    ('图4-1 系统包图',           'fig4-1_package.png', 600),
    ('图4-2 系统类图',           'fig4-2_class.png', 620),
]

# 先解析所有位置
resolved = []
for t, f, w in JOBS:
    idx = find_para(t)
    resolved.append((idx, t, f, w))
    print(('OK ' if idx is not None else 'MISS'), t, idx)

# 从后往前插入
for idx, t, f, w in sorted([x for x in resolved if x[0] is not None], key=lambda x: -x[0]):
    r = call('doc_insert_image', {'idx': idx, 'image_path': FIG + f, 'w': w}, FID)
    print(('IMG-OK ' if 'image_url' in r or 'ok' in r.lower() else 'IMG-FAIL'), t, r[-90:])
