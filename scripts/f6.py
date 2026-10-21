# -*- coding: utf-8 -*-
"""第4章 收尾：剩余标题重命名 + 各模块说明文字"""
import sys
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from rlib import do, replace_text, call

NOTE = '（注释：给出说明文字，根据四层架构控制层、服务层、数据访问层和实体层给出该功能模块的类图、顺序图、活动图和状态图，状态图为选做，其他为必做）'

# 当前剩余标题：**模块设计 (作品与商品已占位)
print('-- headings --')
r = replace_text('**模块设计', '课程与体验模块设计', nth=0)
print('  **模块设计#0 -> 课程与体验模块设计:', r[0])
r = replace_text('**模块设计', '运营与统计模块设计', nth=1)
print('  **模块设计#1 -> 运营与统计模块设计:', r[0])

# 说明文字：4 个 **注释 段落（作品与商品/课程与体验/运营与统计/内容与消息）
print('-- notes --')
do([
 (NOTE, '作品与商品模块负责非遗作品的展示与商品化销售，是平台文化传播与商业转化的基础。控制层包含用户端的 GoodsController、CartController、OrderController，匠人端的 CraftsmanGoodsController，以及管理端的 AdminGoodsController；服务层由 GoodsService、CartService、OrderService 组成，其中 OrderService 通过策略模式调用优惠券计算策略完成订单金额结算，并在事务中完成库存扣减、订单与订单项落库；数据访问层包含 GoodsMapper、GoodsSpecMapper、GoodsImageMapper、CartMapper、OrderMapper、OrderItemMapper；实体层对应 Goods、GoodsSpec、GoodsImage、Cart、Address、Orders、OrderItem 等类。商品下单的主流程顺序与优惠券结算的类结构分别如图 4-9、图 4-10 所示。', 'para'),
 (NOTE, '课程与体验模块是预约业务的前置支撑，负责体验课程的开设、上架与名额管理。控制层包含用户端的 CourseController、匠人端的 CraftsmanCourseController 与管理端的 AdminCourseController；服务层为 CourseService，负责课程的分页检索、详情聚合、名额余量计算、上架下架与排期维护，并通过定时任务自动关闭已过报名截止时间的课程；数据访问层为 CourseMapper；实体层对应 Course 类，其中 quota 表示课程总名额、booked 表示已预约人数，两者之差即前端展示的剩余名额。课程详情聚合了所属匠人、分类、评价统计与当前用户的预约状态，其查询逻辑引入了 Redis 缓存以降低数据库压力。', 'para'),
 (NOTE, '运营与统计模块面向管理员，承担平台内容运营与数据决策的支撑职责。控制层包含 AdminBannerController、AdminArticleController、AdminCategoryController、AdminCouponController、AdminLogController、DashboardController 与 StatController；服务层由 BannerService、ArticleService、CategoryService、CouponService、OperationLogService、DashboardService 与 StatService 组成；数据访问层为各业务 Mapper；实体层对应 Category、Banner、Article、ArticleComment、ArticleLike、Coupon、UserCoupon、OperationLog 等类。统计服务基于 MyBatis-Plus 的聚合查询与 ECharts 可视化，向前端输出预约趋势、商品销量排行、用户增长与优惠券核销率等指标。', 'para'),
 (NOTE, '内容与消息模块负责知识资讯、互动与站内通知。控制层包含 ArticleController、CommentController、MessageController、FavoriteController、FeedbackController；服务层由 MessageService、FavoriteService、FeedbackService 组成，其中 MessageService 在预约状态发生流转时自动向用户推送站内消息，例如"您的预约已通过审核，请按时到店体验"；数据访问层为 MessageMapper、FavoriteMapper、FeedbackMapper，其中 favorite 表通过 type 字段统一管理作品收藏、课程收藏与文章点赞；实体层对应 Message、Favorite、Feedback 类，message 表通过 user_id 与 is_read 字段支持未读消息计数与已读标记。', 'para'),
])
print('CH4 done')
