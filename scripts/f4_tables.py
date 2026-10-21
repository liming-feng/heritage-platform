# -*- coding: utf-8 -*-
"""第3章 表3-1 非遗分类表 / 表3-2 预约信息表"""
import sys, json
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import call

T1 = 'nyal7mdj'
T2 = 'tzz5619t'


def set_cells(tid, rows):
    cells = []
    for r, rowdata in enumerate(rows, start=1):
        for c, v in enumerate(rowdata, start=1):
            cells.append({'row': r, 'col': c, 'text': v})
    r = call('doc_set_table_cells', {'table_id': tid, 'cells': cells})
    print('  set_cells', tid, len(cells), 'ok' in r)


# ---------- 表3-1 非遗分类表 category：原 3 行 × 4 列，需扩展到 8 行 ----------
# 现有: 行1 表头 / 行2 id / 行3 name  → 在行3下方插入 5 行 → 共 8 行
r = call('doc_insert_table_rows', {'table_id': T1,
                                   'row_inserts': [{'anchor_row': 3, 'insert_below': True, 'count': 5}]})
print('T1 insert rows:', r[-120:])

T1_ROWS = [
    ['字段名', '类型', '字段约束', '说明'],
    ['id', 'int', '自增、主键', '分类ID'],
    ['name', 'varchar(50)', '非空', '分类名称，如剪纸、木雕、陶艺'],
    ['icon', 'varchar(255)', '可空', '分类图标地址'],
    ['description', 'varchar(500)', '可空', '分类描述'],
    ['sort', 'int', '默认值为0', '排序值，数值越小越靠前'],
    ['status', 'tinyint', '默认值为1', '状态：1启用、0停用'],
    ['create_time', 'datetime', '非空', '创建时间'],
]
set_cells(T1, T1_ROWS)

# ---------- 表3-2 预约信息表 booking：原 12 行 × 5 列，需扩展到 16 行 ----------
# 在行12下方插入 4 行 → 共 16 行
r = call('doc_insert_table_rows', {'table_id': T2,
                                   'row_inserts': [{'anchor_row': 12, 'insert_below': True, 'count': 4}]})
print('T2 insert rows:', r[-120:])

T2_ROWS = [
    ['字段名', '类型', '允许空', '字段约束', '说明'],
    ['id', 'bigint', '否', '自增、主键', '预约记录ID'],
    ['booking_no', 'varchar(32)', '否', '唯一索引', '预约单号，业务唯一标识'],
    ['course_id', 'bigint', '否', '索引、外键', '所属体验课程ID'],
    ['user_id', 'bigint', '否', '索引、外键', '预约用户ID'],
    ['craftsman_id', 'bigint', '是', '索引、外键', '课程所属匠人ID'],
    ['contact_name', 'varchar(50)', '否', '', '联系人姓名'],
    ['contact_phone', 'varchar(11)', '否', '', '联系人电话'],
    ['quota', 'int', '否', '默认值为1', '预约人数'],
    ['status', 'tinyint', '否', '默认值为0', '状态：0待审核、1已通过、2已拒绝、3已取消、4已核销、5已完成'],
    ['checkin_code', 'varchar(32)', '是', '', '现场核销码'],
    ['checkin_time', 'datetime', '是', '', '核销时间'],
    ['audit_time', 'datetime', '是', '', '审核时间'],
    ['reject_reason', 'varchar(255)', '是', '', '拒绝原因'],
    ['cancel_by', 'tinyint', '是', '', '取消来源：1用户、2匠人、3管理员、4系统'],
    ['create_time', 'datetime', '否', '非空', '创建时间'],
]
set_cells(T2, T2_ROWS)
print('CH3 tables done')
