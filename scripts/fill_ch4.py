# -*- coding: utf-8 -*-
"""第4章 系统详细设计 正文替换"""
import sys
sys.path.insert(0, r'E:/workbuddy1/2026-09-17-10-20-47/非遗平台项目/scripts')
from lib import do_edits

FID = "C:/Users/limingfeng/Desktop/2026_09_08_2.开题报告_姓名学号/工程实训报告_李明峰23100025.docx"

E = []
# 4.4 本章小结
E.append((21157, 21302, '本章围绕系统详细设计展开，按照分层架构对系统进行了细化。首先给出了系统包图，明确了 common、config、security、aspect、module、task 等包的组织方式与依赖关系；随后以类图描述了 Controller、Service、Mapper、Entity、VO/DTO 五层之间的协作关系；最后以预约模块为重点，从控制层、服务层、数据访问层和实体层四个层面给出了类图与顺序图，并通过活动图刻画了预约提交到课程评价的完整业务流程，通过状态图明确了预约单的状态流转规则。详细设计为后续编码实现提供了清晰的依据，也使得系统的可维护性与可扩展性得到保障。'))
# 4.3.4 运营管理模块设计
E.append((21080, 21150, '运营管理模块由优惠券服务、资讯服务、日志服务与统计服务构成。在控制层，管理端的 AdminCouponController 负责优惠券的创建、启停与领取记录查询，AdminArticleController 与 AdminBannerController 负责资讯与轮播的维护，AdminLogController 提供操作日志的筛选查询，AdminStatController 对外暴露各类统计图表的数据源。在服务层，CouponService 采用策略模式处理优惠券抵扣计算，通过工厂类按券类型动态选择 FullReductionStrategy 或 DiscountStrategy 实现，金额统一使用 BigDecimal 保留两位小数。在数据访问层，CouponMapper、ArticleMapper 与 OperationLogMapper 负责相应数据的读写。操作日志由 OperationLogAspect 切面在标注了 @OperationLog 的方法执行前后自动采集操作人、角色、方法、参数、IP 与耗时，并异步写入 operation_log 表，避免阻塞主业务流程。统计服务则从订单、预约、商品等表聚合数据，为管理员提供基于 ECharts 的可视化报表。'))
# 4.3.3 交易模块设计
E.append((21001, 21071, '交易模块由购物车、订单与支付三部分构成。在控制层，PortalCartController 负责购物车的增删改查，PortalOrderController 负责下单、支付、查询、确认收货与退款，CraftsmanOrderController 负责匠人端的发货与退款审核。在服务层，CartService 管理购物车项与选中状态；OrderService 承载下单的核心逻辑，在事务中依次完成地址快照生成、库存校验与扣减、优惠券可用性校验与抵扣计算、订单主表与订单项写入，并通过状态机约束订单状态流转（0 待付款、1 待发货、2 待收货、3 已完成、4 已退款）。在数据访问层，OrdersMapper 与 OrderItemMapper 负责订单数据的持久化，CouponMapper 与 GoodsMapper 配合完成优惠券与库存的联动更新。支付环节采用模拟支付方式，支付成功后写入 pay_time 并生成站内消息通知用户，保证了下单全流程的可演示性与数据一致性。'))
# 4.3.2 商品展示模块设计
E.append((20922, 20992, '作品与商品模块分为展示与维护两条线。在控制层，用户端的 PortalGoodsController 与 PortalCategoryController 提供作品分类、列表、详情、搜索与排序接口，匠人端的 CraftsmanGoodsController 提供商品的发布、编辑、上下架与库存维护，管理端的 AdminGoodsController 与 AdminCategoryController 负责商品审核与分类管理。在服务层，GoodsService 处理商品的查询与缓存逻辑，列表与详情等高热点接口优先访问 Redis 缓存，命中失败时回源数据库并按“先更新数据库、再删除缓存”的策略保证一致性；空结果缓存五分钟以防止缓存穿透，不同页面设置差异化过期时间以防止缓存雪崩。在数据访问层，GoodsMapper、GoodsSpecMapper、GoodsImageMapper 分别操作商品主表、规格表与图集表。作品浏览量通过 Redis 计数器累加，再由定时任务每小时回写 MySQL，减轻数据库写压力。'))
# 4.3.1 预约模块设计（题眼）
E.append((20822, 20822, ''))
E.append((20752, 20822, '预约模块是本课题的核心模块，也是整个系统技术难度最高的部分。在控制层，PortalBookingController 面向用户提供预约提交、我的预约、取消预约与核销码查询接口；CraftsmanBookingController 面向匠人提供报名名单查看、预约审核、现场核销与名单导出接口；AdminBookingController 与 AdminBookingStatController 面向管理员提供全平台预约监管与预约统计接口。在服务层，BookingService 接口定义了预约业务的核心方法，BookingServiceImpl 承担具体实现，其中 submit 方法按“获取分布式锁—行锁校验名额—扣减名额—写入预约记录—释放锁”的顺序执行，audit、cancel、checkin 等方法分别完成审核、取消与核销，并统一在状态变更后写入 booking_status_log 记录流转轨迹。在数据访问层，BookingMapper 通过 SELECT ... FOR UPDATE 实现行级锁，BookingConfigMapper 提供预约规则的读取，BookingStatusLogMapper 记录状态日志。在实体层，Booking 实体承载预约数据，BookingStatusEnum 枚举定义六个预约状态并提供状态流转校验方法，禁止非法跳转。预约模块类图如图 4-3 所示，预约提交顺序图如图 4-4 所示，预约业务流程活动图如图 4-5 所示，预约单状态图如图 4-6 所示。'))
# 4.2 系统类设计
E.append((20736, 20736, ''))
E.append((20727, 20736, '图4-2 系统类图'))
E.append((20725, 20726, ''))
E.append((20724, 20724, ''))
E.append((20718, 20723, ''))
E.append((20708, 20717, '图4-1 系统包图'))
E.append((20706, 20707, ''))
E.append((20682, 20705, '系统包设计遵循分层架构与模块化原则，根包为 com.lmf.heritage，其下按职责划分为若干子包。common 包存放统一响应体 Result、分页封装 PageResult、业务异常 BusinessException、错误码枚举 ErrorCode 与全局异常处理器 GlobalExceptionHandler，为全系统提供统一的返回与异常规范；config 包集中管理跨域、MyBatis-Plus、Redis、WebMvc、Jackson 等配置类；security 包封装 JwtUtil、JwtInterceptor 与自定义注解，统一处理登录校验；aspect 包存放 OperationLogAspect 与限流切面，实现横切关注点；module 包按业务域组织 user、craftsman、goods、trade、course、booking、review、coupon、content、message、log、adminstat 等子模块，每个子模块内部再分为 controller、service、service.impl、mapper、entity、dto、vo 七个子包，保证业务内聚；task 包存放浏览量回写、预约超时自动处理与优惠券过期扫描等定时任务。系统包图如图 4-1 所示。'))
# 4.1 系统包设计（标题保持，正文在 20682 已处理）

do_edits(FID, E)
print('CH4 done:', len(E))

# 4.3 功能模块设计：三个模块标题与 UML 图题
E.append((21151, 21151, ''))
# 图题改名（在已插入/待插入的图位置）
E.append((20914, 20914, ''))
E.append((20902, 20913, '图4-6 预约状态图'))
E.append((20900, 20901, ''))
E.append((20888, 20899, '图4-5 预约业务流程活动图'))
E.append((20886, 20887, ''))
E.append((20873, 20884, '图4-4 预约提交顺序图'))
E.append((20871, 20872, ''))
E.append((20868, 20870, ''))
E.append((20856, 20867, '图4-3 预约模块类图'))
E.append((20854, 20855, ''))
E.append((20842, 20853, ''))
E.append((20840, 20841, ''))
E.append((20838, 20839, ''))
E.append((20825, 20837, ''))
E.append((20823, 20824, ''))
# 三个模块标题
E.append((21073, 21079, '运营管理模块设计'))
E.append((20994, 21000, '商品与交易模块设计'))
E.append((20915, 20921, '预约模块设计'))

do_edits(FID, E)
print('CH4b done:', len(E))
