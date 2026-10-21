# -*- coding: utf-8 -*-
"""第4章 系统详细设计"""
import sys
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import do, replace_text, call

# ---------- 1. 模块标题重命名 ----------
print('-- heading renames --')
r = replace_text('**模块设计', '预约模块设计', nth=0)
print('  nth0 预约模块设计:', r[0])
for nth, newname in [(1, '作品与商品模块设计'), (2, '课程与体验模块设计'), (3, '运营与统计模块设计')]:
    r = replace_text('**模块设计', newname, nth=nth)
    print('  nth%d %s : %s' % (nth, newname, r[0]))
# 修正错别字 "+*模快设计"
r = replace_text('**模快设计', '内容与消息模块设计', nth=0)
print('  **模快设计 ->', r[0])

# ---------- 2. 正文 ----------
print('-- body --')
do([
 # ===== 4.1 系统包设计 =====
 ('（注释：给出说明文字、给出系统实体类图和包图）',
  '本系统后端基于 Spring Boot 3.2.5 构建，工程按照职责分层与业务模块两个维度组织包结构。根包为 com.lmf.heritage，其下包含 common、config、security、aspect、annotation、module、task 七个一级包。common 包存放全局统一响应体 Result、分页封装 PageResult、业务异常 BizException、状态码枚举 ResultCode 与常用工具类；config 包集中管理 MyBatis-Plus、Redis、Web MVC、Knife4j 接口文档、跨域与静态资源映射等配置类；security 包承载 JWT 工具类 JwtUtil、登录用户上下文 UserContext、令牌黑名单管理器 TokenBlacklistManager 以及登录拦截器与刷新令牌逻辑；aspect 包实现面向切面的横切功能，其中 OperationLogAspect 结合 annotation 包中的 @OperationLog 注解完成管理员关键操作的自动留痕；task 包存放基于 Spring Task 的定时任务，用于清理过期预约、自动关闭超时未支付订单以及缓存预热。'),
 ('（注释：给出说明文字、给出系统实体类图和包图）',
  'module 包是业务代码的主体，按业务域进一步划分为 user、craftsman、goods、trade、course、booking、review、coupon、content、message、log、adminstat、dashboard、favorite、feedback、home 十六个子包。每个子包内部严格遵循 controller—service—mapper—entity 的四层划分：controller 负责接收请求与参数校验，service 接口与其 impl 实现类承载业务逻辑，mapper 继承 MyBatis-Plus 的 BaseMapper，entity 与数据库中数据表一一对应，dto 与 vo 则承担层间数据传递与视图渲染的职责。这种"按业务分模块、模块内分层"的包结构使预约、交易等核心业务代码高度内聚，降低了模块间的耦合。系统包结构如图 4-1 所示。',
  'para'),
 # 4.2 系统类设计
 ('图3-8 系统包图', '图4-1 系统包结构图', 'text'),
 ('图3-9 系统类图', '图4-2 系统核心类图', 'text'),
 # 4.3.1 预约模块
 ('（注释：给出说明文字，根据四层架构控制层、服务层、数据访问层和实体层给出该功能模块的类图、顺序图、活动图和状态图，状态图为选做，其他为必做）',
  '预约是本系统的核心业务模块，也是连接用户端、匠人端与管理员端的关键纽带。该模块按照四层架构组织：控制层包含用户端的 BookingController、匠人端的 CraftsmanBookingController 与管理端的 AdminBookingController，分别处理预约提交、匠人审核与管理员监管三类请求；服务层由 BookingService 接口及其实现类 BookingServiceImpl 构成，承载名额校验与扣减、预约单生成、状态流转、核销码校验等核心逻辑；数据访问层为 BookingMapper 与 BookingStatusLogMapper，负责预约记录与状态流转日志的持久化；实体层则包含 Booking、BookingStatusLog、BookingConfig 三个实体类，分别对应预约记录、状态日志与预约规则配置。预约模块的类图如图 4-3 所示。'),
 # ===== 预约模块的图题 =====
 ('图3-11 图书借阅类图', '图4-3 预约模块类图', 'text'),
 ('图3-11 借书顺序图', '图4-4 预约提交顺序图', 'text'),
 ('图3-12 还书顺序图', '图4-5 预约审核与核销顺序图', 'text'),
 ('图3-13 借书活动图', '图4-6 预约业务流程活动图', 'text'),
 ('图3-14 还书活动图', '图4-7 预约名额防超卖活动图', 'text'),
 ('图3-15 图书状态图', '图4-8 预约状态图', 'text'),
])
