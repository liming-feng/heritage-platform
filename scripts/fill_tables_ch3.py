# -*- coding: utf-8 -*-
"""填充第3章数据库表（表3-1 分类表 / 表3-2 预约表）"""
import sys
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from lib import call

FID = "C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"

# ---- 表3-1 非遗分类表：现有 3x4，需扩到 8x4（表头 + 7 字段）----
r = call('doc_insert_table_rows', {
    'table_id': 'gjeqjog7',
    'row_inserts': [{'anchor_row': 3, 'count': 5, 'insert_below': False}]
}, FID)
print('insert cat rows:', r[-140:])

cat = [
 {'row':1,'col':1,'text':'字段名'},{'row':1,'col':2,'text':'类型'},{'row':1,'col':3,'text':'字段约束'},{'row':1,'col':4,'text':'说明'},
 {'row':2,'col':1,'text':'id'},{'row':2,'col':2,'text':'bigint'},{'row':2,'col':3,'text':'主键、自增'},{'row':2,'col':4,'text':'分类ID'},
 {'row':3,'col':1,'text':'name'},{'row':3,'col':2,'text':'varchar(50)'},{'row':3,'col':3,'text':'非空'},{'row':3,'col':4,'text':'分类名称'},
 {'row':4,'col':1,'text':'icon'},{'row':4,'col':2,'text':'varchar(255)'},{'row':4,'col':3,'text':'可空'},{'row':4,'col':4,'text':'分类图标'},
 {'row':5,'col':1,'text':'description'},{'row':5,'col':2,'text':'varchar(255)'},{'row':5,'col':3,'text':'可空'},{'row':5,'col':4,'text':'分类描述'},
 {'row':6,'col':1,'text':'sort'},{'row':6,'col':2,'text':'int'},{'row':6,'col':3,'text':'默认0'},{'row':6,'col':4,'text':'排序值'},
 {'row':7,'col':1,'text':'status'},{'row':7,'col':2,'text':'tinyint'},{'row':7,'col':3,'text':'默认1'},{'row':7,'col':4,'text':'状态（0停用1启用）'},
 {'row':8,'col':1,'text':'create_time'},{'row':8,'col':2,'text':'datetime'},{'row':8,'col':3,'text':'非空'},{'row':8,'col':4,'text':'创建时间'},
]
r = call('doc_set_table_cells', {'table_id':'gjeqjog7','cells':cat}, FID)
print('cat cells:', r[-120:])

# ---- 表3-2 预约信息表：现有 12x5，需扩到 16x5（表头 + 15 字段）----
r = call('doc_insert_table_rows', {
    'table_id': '06syssez',
    'row_inserts': [{'anchor_row': 12, 'count': 4, 'insert_below': False}]
}, FID)
print('insert booking rows:', r[-140:])

bk = [
 {'row':1,'col':1,'text':'字段名'},{'row':1,'col':2,'text':'类型'},{'row':1,'col':3,'text':'允许空'},{'row':1,'col':4,'text':'字段约束'},{'row':1,'col':5,'text':'说明'},
 {'row':2,'col':1,'text':'id'},{'row':2,'col':2,'text':'bigint'},{'row':2,'col':3,'text':'否'},{'row':2,'col':4,'text':'自增、主键'},{'row':2,'col':5,'text':'预约ID'},
 {'row':3,'col':1,'text':'booking_no'},{'row':3,'col':2,'text':'varchar(32)'},{'row':3,'col':3,'text':'否'},{'row':3,'col':4,'text':'唯一'},{'row':3,'col':5,'text':'预约单号'},
 {'row':4,'col':1,'text':'course_id'},{'row':4,'col':2,'text':'bigint'},{'row':4,'col':3,'text':'否'},{'row':4,'col':4,'text':'索引、外键'},{'row':4,'col':5,'text':'课程ID'},
 {'row':5,'col':1,'text':'user_id'},{'row':5,'col':2,'text':'bigint'},{'row':5,'col':3,'text':'否'},{'row':5,'col':4,'text':'索引、外键'},{'row':5,'col':5,'text':'预约用户ID'},
 {'row':6,'col':1,'text':'contact_name'},{'row':6,'col':2,'text':'varchar(50)'},{'row':6,'col':3,'text':'否'},{'row':6,'col':4,'text':''},{'row':6,'col':5,'text':'联系人姓名'},
 {'row':7,'col':1,'text':'contact_phone'},{'row':7,'col':2,'text':'varchar(11)'},{'row':7,'col':3,'text':'否'},{'row':7,'col':4,'text':''},{'row':7,'col':5,'text':'联系电话'},
 {'row':8,'col':1,'text':'quota'},{'row':8,'col':2,'text':'int'},{'row':8,'col':3,'text':'否'},{'row':8,'col':4,'text':'默认1'},{'row':8,'col':5,'text':'预约人数'},
 {'row':9,'col':1,'text':'status'},{'row':9,'col':2,'text':'tinyint'},{'row':9,'col':3,'text':'否'},{'row':9,'col':4,'text':'默认0'},{'row':9,'col':5,'text':'状态（0待审核1已通过2已拒绝3已取消4已核销5已完成）'},
 {'row':10,'col':1,'text':'checkin_code'},{'row':10,'col':2,'text':'varchar(32)'},{'row':10,'col':3,'text':'是'},{'row':10,'col':4,'text':'唯一'},{'row':10,'col':5,'text':'核销码'},
 {'row':11,'col':1,'text':'checkin_time'},{'row':11,'col':2,'text':'datetime'},{'row':11,'col':3,'text':'是'},{'row':11,'col':4,'text':''},{'row':11,'col':5,'text':'核销时间'},
 {'row':12,'col':1,'text':'audit_time'},{'row':12,'col':2,'text':'datetime'},{'row':12,'col':3,'text':'是'},{'row':12,'col':4,'text':''},{'row':12,'col':5,'text':'审核时间'},
 {'row':13,'col':1,'text':'reject_reason'},{'row':13,'col':2,'text':'varchar(255)'},{'row':13,'col':3,'text':'是'},{'row':13,'col':4,'text':''},{'row':13,'col':5,'text':'拒绝原因'},
 {'row':14,'col':1,'text':'cancel_reason'},{'row':14,'col':2,'text':'varchar(255)'},{'row':14,'col':3,'text':'是'},{'row':14,'col':4,'text':''},{'row':14,'col':5,'text':'取消原因'},
 {'row':15,'col':1,'text':'cancel_by'},{'row':15,'col':2,'text':'tinyint'},{'row':15,'col':3,'text':'是'},{'row':15,'col':4,'text':''},{'row':15,'col':5,'text':'取消来源（1用户2匠人3管理员4系统）'},
 {'row':16,'col':1,'text':'create_time'},{'row':16,'col':2,'text':'datetime'},{'row':16,'col':3,'text':'否'},{'row':16,'col':4,'text':'非空'},{'row':16,'col':5,'text':'创建时间'},
]
r = call('doc_set_table_cells', {'table_id':'06syssez','cells':bk}, FID)
print('booking cells:', r[-120:])
