# -*- coding: utf-8 -*-
"""替换文档中的架构图为自生成配图"""
import sys, json
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import call

FIGS = r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/report_figs'

# idx 取自 doc_get_images，与图题配对
# 注意：此刻先做一次最新快照
r = call('doc_get_images', {})
d = json.loads(r[r.find('{'):])
imgs = {}
for im in d.get('images', []):
    imgs[im['index']] = im['image_url']

print('images in doc:', len(imgs))

# 目标：把架构类图题前的图片替换为对应自生成图
PAIRS = [
 # (图片 idx, 新图文件)
 (16977, 'fig3-1_deploy.png'),    # 图3-1 系统拓扑图
 (17202, 'fig3-2_logic.png'),     # 图3-2 系统部署图
 (17663, 'fig3-2_logic.png'),     # 图3-2 软件逻辑架构图
 (19041, 'fig3-3_function_tree.png'),  # 图3-3 系统功能结构图
 (19753, 'fig3-9_er.png'),        # 图3-6 活动实体E-R图
 (22508, 'fig4-1_package.png'),   # 图4-1
 (22529, 'fig4-2_class.png'),     # 图4-2
 (22977, 'fig4-3_booking_class.png'),
 (22993, 'fig4-4_booking_seq.png'),
 (23008, 'fig4-5_booking_activity.png'),
 (23029, 'fig4-7_antoversell.png'),
 (23047, 'fig4-7_antoversell.png'),
 (23065, 'fig4-6_booking_state.png'),
]
for idx, fn in PAIRS:
    if idx not in imgs:
        print('MISS', idx)
        continue
    p = FIGS + '/' + fn
    rr = call('doc_replace_image', {'idx': idx, 'old_image_url': imgs[idx], 'new_image_path': p})
    print(idx, fn, 'ok' in rr)
print('DONE')
