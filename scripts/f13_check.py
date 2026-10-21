# -*- coding: utf-8 -*-
"""最终收尾：修正目录残留条目 + 结论 p803 段落"""
import sys
sys.path.insert(0, r"E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts")
import rlib
from rlib import call, find, struct, para_range, replace_text, replace_para

print('=== 健康检查 ===')
nodes = struct()
print('节点数:', len(nodes))

for a in ['1.5.1 微信小程序', '而管理者端则能实现用户管理']:
    locs = find(a)
    print(repr(a), '->', locs)
