# -*- coding: utf-8 -*-
"""按锚点定位填充：封面 + 摘要 + 第1章 + 第2章"""
import sys
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import do, replace_para, replace_text, call, FID

# ========== 封面 ==========
call('doc_set_table_cells', {'table_id': 'gegy7d2h', 'cells': [
    {'row':1,'col':2,'text':'李明峰'},{'row':2,'col':2,'text':'23100025'},
    {'row':3,'col':2,'text':'计算机科学与技术'},{'row':4,'col':2,'text':'2023级大数据班'},
    {'row':5,'col':2,'text':'吴长伟'},{'row':6,'col':2,'text':'信息工程学院'},
    {'row':7,'col':2,'text':'全栈方向'}]})
replace_text('题目：***', '题目：基于SpringBoot+Vue的非遗手工艺品展示与预约体验平台设计与实现')
print('封面 OK')

# ========== 摘要 ==========
do([
 ('（注释：中文摘要内容', ' '),
 ('由于**，所以开发以**变得特别重要了。',
  '随着现代化进程加快，剪纸、木雕、陶艺、刺绣等传统技艺面临传承人老龄化、展示渠道单一、产品销售依赖线下门店与展会等现实困境，公众了解、购买与体验非遗的需求难以得到有效满足。因此，开发一个集作品展示、手工商城、匠人推介与线下体验预约于一体的信息化平台就显得特别重要。'),
 ('本课题系统基于**，采用前后端分离架构',
  '本课题系统基于SpringBoot 3.2.5与Vue 3.4的前后端分离架构，采用MySQL 8存储数据、Redis 7承担缓存与分布式锁，优化了非遗作品与体验课程的运营管理流程。采用文献研究法、需求分析法、原型设计法和对比实验法，结合现有技术，对预约名额并发超卖、登录认证与接口安全、热点数据缓存一致性、优惠券抵扣计算扩展性等技术难题进行分析和解决。系统实现攻克三层防超卖、双Token无感续期、AOP操作日志等难点，经JUnit单元测试、Postman接口测试、JMeter并发压测与多浏览器兼容性测试验证，在响应速度、并发能力和数据一致性方面表现良好。最终实现用户可通过浏览器便捷完成作品浏览、商品购买、优惠券领取与体验课程在线预约，匠人可自主管理作品商品与预约审核核销，管理员可完成资质审核、预约监管、规则配置与数据可视化分析等功能，为非遗数字化传承与活化利用提供一个高效、便捷、安全的运营管理工具，提升非遗文化传播与匠人增收的整体效能。'),
 ('（注释：关键词内容', ' '),
 ('关键词：任务背景；技术1；技术2；框架', '关键词：非遗手工艺品；SpringBoot；Vue；预约体验'),
 ('** System', 'Design and Implementation of an Intangible Cultural Heritage Handicraft Display and Booking Experience Platform Based on SpringBoot and Vue', 'text'),
 ('（注释：英文摘要和关键词', ' '),
 ('Due to t**y important to develop', 'Due to the fact that some intangible cultural heritage crafts are facing problems such as an aging group of inheritors, single display channels, and product sales relying on offline stores and exhibitions, it is difficult for the public to understand, purchase and experience intangible cultural heritage, so it is particularly important to develop an integrated platform for heritage display and booking experience.'),
 ('The system ****universities.', 'The system is developed based on SpringBoot and Vue with a front-end/back-end separated architecture, adopting MySQL for data persistence and Redis for caching and distributed locking. It implements integrated functions such as heritage display, an online handicraft mall, craftsman homepages and online booking of offline experience courses, and solves engineering problems such as concurrent booking overselling, two-token authentication and coupon deduction calculation.'),
 ('Key words:** ; ** ; ** ; **t', 'Key words: Intangible Cultural Heritage; SpringBoot; Vue; Booking Experience'),
])
print('摘要 OK')

# ========== 第1章 绪论 ==========
do([
 ('现代高校规*，数据容易出错或者信息丢失', '非物质文化遗产是中华民族的文化瑰宝，但部分非遗技艺面临传承人老龄化、展示渠道单一、产品销售依赖线下门店和展会等困境。传统模式下，非遗作品多以图文介绍的形式零散展示，缺少完整的商品规格、库存、订单与售后能力；线下体验课的报名多依赖微信群或电话，名额管理混乱且缺少审核与通知机制；匠人的主体地位不突出，其师承、履历与代表作难以系统呈现；同时平台的浏览量、销量、预约热度等运营数据也难以被有效采集与利用，导致数据价值挖掘不足。公众了解、购买与体验非遗的需求，与非遗资源的数字化、平台化供给之间仍存在明显差距。'),
 ('开发个基于微信小程序的*****，而管理者管理活动和数据也更高效。',
  '开发一个集作品展示、手工商城、匠人主页与体验课程在线预约于一体的非遗手工艺品展示与预约体验平台，既能拓宽非遗作品的线上销路、把“旁观式”的文化消费转化为“参与式”的体验消费，也能让管理者高效地完成内容审核、预约监管与数据统计，从而更好地满足非遗展示与预约体验管理的高效化、信息化需求。'),
])
# 1.2 目的与意义（第二处相同锚点）
do([
 ('现代高校规*，数据容易出错或者信息丢失',
  '非物质文化遗产是中华民族在长期生产生活实践中创造并世代相传的文化瑰宝，承载着民族的思维方式、审美情趣与工艺智慧。然而随着现代化进程加快，部分非遗技艺面临传承人老龄化、市场认知度低、产品销售渠道单一等现实困境，传统的线下门店与展会销售模式客群半径有限，非遗展示与体验的组织也多依赖微信群或电话沟通，信息分散、名额管理混乱、数据难以沉淀。因此，借助数字技术为非遗开辟新的传播与变现路径，建设一个信息化、规范化的展示与预约体验平台，已成为文化保护与产业发展共同关注的问题。'),
 ('开发个基于微信小程序的*****，而管理者管理活动和数据也更高效。',
  '开发一个面向普通公众、非遗匠人与平台管理者三类角色的展示与预约体验平台，既能帮助公众便捷地了解、购买与体验非遗，也能让匠人低成本地自主运营线上店铺与线下体验业务，同时为平台管理者提供高效的内容审核、预约监管与数据分析手段，具有明确的应用价值与现实意义。'),
])
print('1.1/1.2 OK')
