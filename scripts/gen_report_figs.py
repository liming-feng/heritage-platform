# -*- coding: utf-8 -*-
"""
生成工程实训报告所需配图（SVG -> PNG）。
图：系统功能结构图、硬件部署架构图、软件逻辑架构图、前后端交互时序图、
    数据库E-R图、系统包图、系统类图、预约模块类图/顺序图/活动图、
    预约状态机图、三层防超卖机制图、优惠券策略模式类图。
"""
import os, hashlib, subprocess, sys

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'report_figs'))
os.makedirs(OUT, exist_ok=True)

FONT = "'Microsoft YaHei','微软雅黑','SimHei',sans-serif"
BLUE, BLUE_L = '#2f5fa8', '#e8eff9'
GREEN, GREEN_L = '#3f8f5f', '#e9f5ee'
ORANGE, ORANGE_L = '#c9793a', '#fdf1e6'
PURPLE, PURPLE_L = '#7a5aa8', '#f1ecf9'
GREY, GREY_L = '#5b6b7c', '#eef2f6'
RED, RED_L = '#b5453f', '#fbeceb'
LINE = '#44576b'


def svg(name, w, h, body):
    s = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
         f'viewBox="0 0 {w} {h}">\n'
         f'<defs><style>text{{font-family:{FONT};}}</style>'
         f'<marker id="ar" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">'
         f'<path d="M0,0 L10,4 L0,8 z" fill="{LINE}"/></marker>'
         f'<marker id="arw" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">'
         f'<path d="M0,0 L10,4 L0,8 z" fill="#ffffff"/></marker>'
         f'</defs>\n'
         f'<rect width="{w}" height="{h}" fill="#ffffff"/>\n{body}\n</svg>')
    p = os.path.join(OUT, name + '.svg')
    open(p, 'w', encoding='utf-8').write(s)
    return p


def box(x, y, w, h, text, fill=BLUE_L, stroke=BLUE, fs=14, bold=False, rx=6, tc=None):
    tc = tc or stroke
    fw = ' font-weight="bold"' if bold else ''
    lines = text.split('\n')
    tspans = ''.join(
        f'<tspan x="{x + w/2}" dy="{0 if i == 0 else fs*1.35}">{t}</tspan>'
        for i, t in enumerate(lines))
    oy = y + h/2 - (len(lines)-1)*fs*1.35/2 + fs*0.36
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1.6"/>\n'
            f'<text x="{x + w/2}" y="{oy}" text-anchor="middle" font-size="{fs}" '
            f'fill="{tc}"{fw}>{tspans}</text>\n')


def arrow(x1, y1, x2, y2, label='', dash=False, color=LINE, lw=1.6, fs=12, lbg='#ffffff'):
    d = ' stroke-dasharray="6,4"' if dash else ''
    s = (f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" '
         f'stroke-width="{lw}"{d} marker-end="url(#ar)"/>\n')
    if label:
        mx, my = (x1+x2)/2, (y1+y2)/2
        tw = len(label)*fs*0.62 + 8
        s += (f'<rect x="{mx-tw/2}" y="{my-fs*0.75}" width="{tw}" height="{fs*1.4}" '
              f'fill="{lbg}" opacity="0.92"/>\n'
              f'<text x="{mx}" y="{my+fs*0.35}" text-anchor="middle" font-size="{fs}" '
              f'fill="{color}">{label}</text>\n')
    return s


def poly(pts, label='', fs=12, color=LINE):
    p = ' '.join(f'{x},{y}' for x, y in pts)
    s = f'<polyline points="{p}" fill="none" stroke="{color}" stroke-width="1.6" marker-end="url(#ar)"/>\n'
    if label:
        x, y = pts[len(pts)//2]
        s += f'<text x="{x+8}" y="{y-6}" font-size="{fs}" fill="{color}">{label}</text>\n'
    return s


def title(x, y, t, fs=15, color='#22364a'):
    return f'<text x="{x}" y="{y}" font-size="{fs}" font-weight="bold" fill="{color}">{t}</text>\n'


# ============ 图1 系统功能结构图 ============
def fig_function_tree():
    W, H = 1280, 760
    b = ''
    b += box(490, 18, 300, 48, '非遗手工艺品展示与预约体验平台', fill=BLUE, stroke=BLUE, fs=16, bold=True, tc='#ffffff')
    subs = [
        ('用户端', GREEN, GREEN_L, [
            '注册登录', '首页推荐', '作品展示', '手工商城', '购物车结算', '优惠券',
            '订单与售后', '体验课程', '在线预约', '我的预约', '匠人主页', '评价互动',
            '资讯公告', '消息中心', '浏览历史']),
        ('匠人端', ORANGE, ORANGE_L, [
            '入驻申请', '商品管理', '规格库存', '课程管理', '预约审核',
            '核销签到', '订单发货', '评价回复', '销售看板']),
        ('管理端', PURPLE, PURPLE_L, [
            '用户管理', '资质审核', '审核中心', '分类管理', '资讯轮播',
            '优惠券运营', '预约监管', '规则配置', '数据可视化', '操作日志']),
    ]
    xs = [60, 480, 900]
    bw = 320
    for (name, c, cl, items), x in zip(subs, xs):
        b += box(x, 110, bw, 46, name, fill=c, stroke=c, fs=15, bold=True, tc='#ffffff')
        b += arrow(x + bw/2, 66, x + bw/2, 108)
        n = len(items)
        cols = 2 if n > 8 else 1
        per = (n + cols - 1)//cols
        iw, ih = (bw-14)/cols - 8, 32
        for i, it in enumerate(items):
            cc, rr = i // per, i % per
            ix = x + 6 + cc*(iw+10)
            iy = 172 + rr*(ih+9)
            b += box(ix, iy, iw, ih, it, fill=cl, stroke=c, fs=12.5, rx=4)
        b += arrow(x + bw/2, 156 + 0, x + 12, 172 - 4) if False else ''
    # 顶部到三列的连线
    for x in xs:
        b += poly([(640, 66), (640, 88), (x + bw/2, 88), (x + bw/2, 108)], color=LINE)
    return svg('fig3-3_function_tree', W, H, b)


# ============ 图2 硬件部署架构图 ============
def fig_deploy():
    W, H = 1180, 640
    b = ''
    b += box(40, 60, 240, 100, '用户浏览器\n（Chrome / Edge）', fill=GREY_L, stroke=GREY, fs=13)
    b += box(40, 220, 240, 100, '移动端浏览器\n（响应式访问）', fill=GREY_L, stroke=GREY, fs=13)
    b += box(40, 380, 240, 110, '运维终端\n（SSH / 管理后台）', fill=GREY_L, stroke=GREY, fs=13)
    b += box(400, 60, 340, 48, 'Nginx  Web 服务器', fill=GREEN_L, stroke=GREEN, fs=15, bold=True)
    b += box(400, 150, 340, 430, '', fill='#fbfdff', stroke=BLUE, rx=10)
    b += title(418, 178, '应用服务器（单台云主机 / 本地服务器）', 14)
    b += box(425, 200, 290, 76, 'Nginx 静态资源\n（heritage-web 打包产物 dist）', fill=GREEN_L, stroke=GREEN, fs=12.5)
    b += box(425, 296, 290, 76, 'Spring Boot 应用\nheritage-server.jar  :8080', fill=BLUE_L, stroke=BLUE, fs=12.5)
    b += box(425, 392, 290, 76, 'MySQL 8.0\nheritage 数据库 :3307', fill=ORANGE_L, stroke=ORANGE, fs=12.5)
    b += box(425, 488, 290, 72, 'Redis 7\n缓存 / 分布式锁 :6379', fill=RED_L, stroke=RED, fs=12.5)
    b += box(870, 200, 270, 76, '本地文件存储\nuploads/ 图片目录', fill=PURPLE_L, stroke=PURPLE, fs=12.5)
    b += box(870, 320, 270, 100, '浏览器端能力\nLocalStorage（Token）\nECharts 图表渲染', fill=GREY_L, stroke=GREY, fs=12.5)
    b += box(870, 460, 270, 100, '开发与测试工具\nJDK17 / Maven / Node20\nPostman / JMeter', fill=GREY_L, stroke=GREY, fs=12.5)
    b += arrow(280, 110, 396, 90, 'HTTP/HTTPS')
    b += arrow(280, 270, 396, 250, 'HTTP/HTTPS')
    b += arrow(280, 435, 396, 224, '')
    b += arrow(570, 110, 570, 198, '反向代理')
    b += poly([(715, 238), (830, 238)], '静态资源', color=LINE)
    b += poly([(570, 372), (570, 296)], 'JDBC', color=LINE)
    b += poly([(570, 468), (570, 392)], '', color=LINE)
    b += poly([(715, 334), (830, 334)], '', color=LINE)
    b += poly([(715, 430), (830, 440)], '文件读写', color=LINE)
    return svg('fig3-1_deploy', W, H, b)


# ============ 图3 软件逻辑架构图 ============
def fig_logic():
    W, H = 1180, 700
    b = ''
    layers = [
        ('表现层（Presentation Layer）', GREEN, GREEN_L, 60,
         ['Vue 3.4 + Vite 5', 'Element Plus 组件库', 'Pinia 状态管理', 'Vue Router 路由守卫', 'Axios 请求封装', 'ECharts 可视化']),
        ('接口层（Controller Layer）', BLUE, BLUE_L, 200,
         ['用户端接口', '匠人端接口', '管理端接口', '统一响应体 Result', '参数校验注解', 'Knife4j 接口文档']),
        ('业务层（Service Layer）', ORANGE, ORANGE_L, 340,
         ['用户与权限服务', '商品与交易服务', '预约服务\n（三层防超卖）', '优惠券策略服务', '内容与运营服务', '统计与日志服务']),
        ('数据层（Mapper Layer）', PURPLE, PURPLE_L, 480,
         ['MyBatis-Plus 3.5.7', 'Lambda 条件构造器', '分页插件', '逻辑删除', '自动填充']),
        ('存储层（Storage Layer）', RED, RED_L, 600,
         ['MySQL 8.0 业务数据', 'Redis 7 缓存/锁/黑名单', '本地文件系统 uploads/']),
    ]
    for name, c, cl, y, items in layers:
        b += box(60, y, 250, 100, name.replace('（', '\n（'), fill=c, stroke=c, fs=13.5, bold=True, tc='#ffffff')
        n = len(items)
        iw = 150
        startx = 350
        for i, it in enumerate(items):
            r, cc = i // 4, i % 4
            b += box(startx + cc*(iw+15), y + 10 + r*44, iw, 36, it, fill=cl, stroke=c, fs=11.5, rx=4)
        if y < 600:
            b += arrow(185, y + 100, 185, y + 198)
    b += '<text x="200" y="' + str(680) + '" font-size="12" fill="#44576b" text-anchor="middle">请求下行 / 数据上行</text>'
    return svg('fig3-2_logic', W, H, b)


# ============ 图4 前后端交互时序图 ============
def fig_seq_flow():
    W, H = 1240, 660
    objs = [('浏览器 / Vue 组件', 90), ('Axios 请求拦截器', 330), ('Spring Boot 拦截器', 560), ('Controller', 780), ('Service', 980), ('MySQL / Redis', 1170)]
    b = ''
    for name, x in objs:
        b += box(x - 95, 30, 190, 46, name, fill=BLUE_L, stroke=BLUE, fs=12, bold=True)
        b += f'<line x1="{x}" y1="76" x2="{x}" y2="610" stroke="{LINE}" stroke-width="1" stroke-dasharray="5,5"/>\n'
    steps = [
        (1, 0, 1, '用户触发操作，发起请求'),
        (2, 1, 2, '携带 access_token'),
        (3, 2, 2, 'JWT 校验 + 限流校验'),
        (4, 2, 3, '校验通过，路由到 Controller'),
        (5, 3, 4, '参数校验 @Valid，调用业务方法'),
        (6, 4, 5, '缓存查询 / 加锁扣减名额'),
        (7, 5, 4, '返回业务数据'),
        (8, 4, 3, '封装 Result&lt;T&gt;'),
        (9, 3, 1, '返回 JSON 响应'),
        (10, 1, 0, '响应拦截器处理，更新页面'),
    ]
    y = 110
    for _, a, c, label in steps:
        x1, x2 = objs[a][1], objs[c][1]
        col = GREEN if x1 < x2 else ORANGE
        b += arrow(x1, y, x2, y, label, color=col)
        y += 48
    return svg('fig3-4_seq_flow', W, H, b)


# ============ 图5 数据库 E-R 图 ============
def fig_er():
    W, H = 1420, 880
    b = ''
    ents = {
        'user':          (560, 40, 240, 54, '用户 user', BLUE, BLUE_L),
        'craftsman':     (1080, 40, 240, 54, '匠人 craftsman', ORANGE, ORANGE_L),
        'category':      (60, 170, 220, 54, '非遗分类 category', PURPLE, PURPLE_L),
        'goods':         (60, 320, 220, 54, '商品 goods', ORANGE, ORANGE_L),
        'goods_spec':    (60, 470, 220, 54, '商品规格 goods_spec', ORANGE, ORANGE_L),
        'course':        (1080, 200, 240, 54, '体验课程 course', GREEN, GREEN_L),
        'booking':       (770, 320, 240, 54, '预约 booking', GREEN, GREEN_L),
        'orders':        (330, 470, 240, 54, '订单 orders', BLUE, BLUE_L),
        'order_item':    (330, 620, 240, 54, '订单项 order_item', BLUE, BLUE_L),
        'review':        (770, 620, 240, 54, '评价 review', PURPLE, PURPLE_L),
        'coupon':        (560, 620, 200, 54, '优惠券 coupon', RED, RED_L),
        'user_coupon':   (560, 760, 220, 54, '用户领券 user_coupon', RED, RED_L),
        'article':       (1080, 470, 240, 54, '资讯 article', GREY, GREY_L),
        'banner':        (1080, 620, 240, 54, '轮播图 banner', GREY, GREY_L),
        'message':       (60, 760, 240, 54, '站内消息 message', GREY, GREY_L),
        'favorite':      (330, 760, 240, 54, '收藏 favorite', GREY, GREY_L),
    }
    for k, (x, y, w, h, t, c, cl) in ents.items():
        b += box(x, y, w, h, t, fill=cl, stroke=c, fs=13, bold=True)

    rels = [
        ('user', 'orders', '1', 'n', '下单'),
        ('user', 'booking', '1', 'n', '提交'),
        ('user', 'review', '1', 'n', '发表'),
        ('user', 'craftsman', '1', '1', '扩展'),
        ('user', 'message', '1', 'n', '接收'),
        ('user', 'favorite', '1', 'n', '收藏'),
        ('user', 'user_coupon', '1', 'n', '领取'),
        ('craftsman', 'goods', '1', 'n', '发布'),
        ('craftsman', 'course', '1', 'n', '开设'),
        ('category', 'goods', '1', 'n', '归属'),
        ('goods', 'goods_spec', '1', 'n', '包含'),
        ('goods', 'review', '1', 'n', '被评价'),
        ('course', 'booking', '1', 'n', '被预约'),
        ('course', 'review', '1', 'n', '被评价'),
        ('booking', 'user', 'n', '1', '归属'),
        ('orders', 'order_item', '1', 'n', '包含'),
        ('coupon', 'user_coupon', '1', 'n', '发放'),
        ('article', 'banner', '1', 'n', '运营'),
    ]
    def center(k):
        x, y, w, h = ents[k][:4]
        return (x + w/2, y + h/2)

    def link(a, bkey, la, lb, label):
        (x1, y1), (x2, y2) = center(a), center(bkey)
        b_ = arrow(x1, y1, x2, y2, label, color=LINE, lw=1.3, fs=11)
        return b_
    for a, bk, la, lb, lab in rels:
        b += link(a, bk, la, lb, lab)
    b += title(40, 30, '图 3-9  系统实体联系 E-R 图（主要实体与联系）', 15)
    return svg('fig3-9_er', W, H, b)


# ============ 图6 系统包图 ============
def fig_package():
    W, H = 1180, 620
    b = ''
    b += box(430, 20, 320, 46, 'com.lmf.heritage', fill=BLUE, stroke=BLUE, fs=15, bold=True, tc='#ffffff')
    groups = [
        ('common', '统一响应 Result\n业务异常与错误码\n全局异常处理\n分页封装', BLUE, BLUE_L, 40, 110),
        ('config', 'CorsConfig\nMybatisPlusConfig\nRedisConfig\nWebMvcConfig', GREEN, GREEN_L, 320, 110),
        ('security', 'JwtUtil\nJwtInterceptor\nLoginRequired', ORANGE, ORANGE_L, 600, 110),
        ('aspect', 'OperationLogAspect\nRateLimitAspect', PURPLE, PURPLE_L, 880, 110),
        ('module', 'user / craftsman\ngoods / trade / course\nbooking / review / coupon\ncontent / message / log', RED, RED_L, 40, 300),
        ('task', '浏览量定时回写\n预约超时自动处理\n优惠券过期扫描', GREY, GREY_L, 480, 300),
        ('dto / vo', '请求参数对象\n响应视图对象', GREEN, GREEN_L, 880, 300),
    ]
    for name, content, c, cl, x, y in groups:
        b += box(x, y, 250, 52, name, fill=c, stroke=c, fs=14, bold=True, tc='#ffffff')
        b += box(x, y + 52, 250, 100, content, fill=cl, stroke=c, fs=12)
        b += arrow(x + 125, 66, x + 125, y - 2)
    return svg('fig4-1_package', W, H, b)


# ============ 图7 系统类图（分层） ============
def fig_class():
    W, H = 1240, 620
    b = ''
    cols = [
        ('Controller 层', ['PortalBookingController', 'CraftsmanBookingController', 'AdminBookingController', 'PortalOrderController', 'AdminGoodsController'], BLUE, BLUE_L, 60),
        ('Service 层', ['BookingService', 'OrderService', 'GoodsService', 'CouponService', 'BookingConfigService'], GREEN, GREEN_L, 300),
        ('Mapper 层', ['BookingMapper', 'OrdersMapper', 'GoodsMapper', 'UserMapper', 'CouponMapper'], ORANGE, ORANGE_L, 540),
        ('Entity 层', ['Booking', 'Course', 'Orders', 'Goods', 'User'], PURPLE, PURPLE_L, 780),
        ('VO / DTO', ['BookingVO', 'BookingSubmitDTO', 'BookingAuditDTO', 'AdminBookingStatVO', 'CheckinResultVO'], RED, RED_L, 1020),
    ]
    for name, items, c, cl, x in cols:
        b += box(x, 40, 200, 48, name, fill=c, stroke=c, fs=13.5, bold=True, tc='#ffffff')
        for i, it in enumerate(items):
            b += box(x, 104 + i*80, 200, 62, it, fill=cl, stroke=c, fs=12)
        if x < 1020:
            b += arrow(x + 200, 64, x + 238, 64, color=LINE, lw=1.4)
    for x, lab in [(160, '调用'), (400, '调用'), (640, '映射'), (880, '封装')]:
        b += f'<text x="{x+20}" y="36" font-size="12" fill="{LINE}">{lab}</text>\n'
    b += title(60, 600, '说明：Controller 只做参数校验与响应封装，业务逻辑集中在 Service，数据访问通过 Mapper 操作 Entity，对外输出 VO。', 12.5)
    return svg('fig4-2_class', W, H, b)


# ============ 图8 预约模块类图 ============
def fig_booking_class():
    W, H = 1240, 700
    b = ''

    def cls(x, y, w, name, attrs, meths, c, cl):
        s = box(x, y, w, 44, name, fill=c, stroke=c, fs=13.5, bold=True, tc='#ffffff')
        ah = len(attrs)*18 + 16
        mh = len(meths)*18 + 16
        s += f'<rect x="{x}" y="{y+44}" width="{w}" height="{ah+mh}" fill="#ffffff" stroke="{c}" stroke-width="1.6"/>\n'
        yy = y + 44 + 20
        for a in attrs:
            s += f'<text x="{x+12}" y="{yy}" font-size="11.5" fill="#2b3d4f">{a}</text>\n'
            yy += 18
        s += f'<line x1="{x}" y1="{yy-8}" x2="{x+w}" y2="{yy-8}" stroke="{c}" stroke-width="1"/>\n'
        yy += 10
        for m in meths:
            s += f'<text x="{x+12}" y="{yy}" font-size="11.5" fill="#2b3d4f">{m}</text>\n'
            yy += 18
        return s

    b += cls(60, 60, 280, 'PortalBookingController',
             ['- bookingService : BookingService'],
             ['+ submit(BookingSubmitDTO)', '+ mine(userId)', '+ cancel(id, reason)', '+ checkinCode(id)'], BLUE, BLUE_L)
    b += cls(60, 380, 280, 'CraftsmanBookingController',
             ['- bookingService : BookingService'],
             ['+ roster(query)', '+ audit(BookingAuditDTO)', '+ checkin(BookingCheckinDTO)', '+ export()'], ORANGE, ORANGE_L)
    b += cls(60, 640, 280, 'AdminBookingController', [], [], PURPLE, PURPLE_L)
    b += cls(480, 60, 320, 'BookingService（接口）',
             [],
             ['+ submit(userId, dto) : BookingVO', '+ audit(craftsmanId, dto)',
              '+ cancel(cancelBy, dto)', '+ checkin(code) : CheckinResultVO',
              '+ pageMine(userId, query)', '+ autoRejectTimeout()'], GREEN, GREEN_L)
    b += cls(480, 480, 320, 'BookingServiceImpl',
             ['- bookingMapper : BookingMapper', '- redisTemplate', '- configService'],
             ['- tryLock(courseId, requestId)', '- releaseLock(key, requestId)',
              '- deductQuota(courseId, n)', '- restoreQuota(courseId, n)', '- sendNotice(userId, msg)'], GREEN, GREEN_L)
    b += cls(920, 60, 280, 'Booking（实体）',
             ['- id : Long', '- bookingNo : String', '- courseId : Long', '- userId : Long',
              '- quota : Integer', '- status : Integer', '- checkinCode : String',
              '- checkinTime : LocalDateTime', '- cancelBy : Integer'],
             ['+ isCancelable()', '+ canTransferTo(target)'], RED, RED_L)
    b += cls(920, 420, 280, 'BookingStatusEnum',
             ['WAIT_AUDIT(0)', 'PASSED(1)', 'REJECTED(2)', 'CANCELED(3)', 'CHECKED(4)', 'FINISHED(5)'],
             ['+ transfer(current, target)'], PURPLE, PURPLE_L)
    b += arrow(340, 100, 476, 100, '调用')
    b += arrow(340, 420, 476, 200, '调用')
    b += arrow(640, 300, 640, 476, 'implements', color=LINE, lw=1.3)
    b += arrow(800, 130, 916, 130, '返回', color=LINE, lw=1.3)
    b += arrow(800, 560, 916, 470, '校验流转', color=LINE, lw=1.3)
    return svg('fig4-3_booking_class', W, H, b)


# ============ 图9 预约顺序图 ============
def fig_booking_seq():
    W, H = 1280, 720
    b = ''
    objs = [('用户', 80), ('PortalBooking\nController', 270), ('BookingService\nImpl', 500), ('Redis\n分布式锁', 730), ('BookingMapper/\nMySQL', 960), ('Message\nService', 1190)]
    for name, x in objs:
        b += box(x - 90, 24, 180, 56, name, fill=BLUE_L, stroke=BLUE, fs=12, bold=True)
        b += f'<line x1="{x}" y1="80" x2="{x}" y2="670" stroke="{LINE}" stroke-width="1" stroke-dasharray="5,5"/>\n'
    steps = [
        (0, 1, '1. 提交预约', GREEN),
        (1, 2, '2. submit(userId, dto)', GREEN),
        (2, 3, '3. SET lock NX PX 10000', ORANGE),
        (3, 2, '4. 加锁成功', ORANGE),
        (2, 4, '5. SELECT remain_quota FOR UPDATE', BLUE),
        (4, 2, '6. 返回剩余名额', BLUE),
        (2, 4, '7. 扣减名额 + 插入预约记录', BLUE),
        (4, 2, '8. 事务提交（唯一索引校验）', BLUE),
        (2, 5, '9. 发送预约待审核通知', PURPLE),
        (2, 3, '10. Lua 脚本释放锁', ORANGE),
        (2, 1, '11. 返回 BookingVO', GREEN),
        (1, 0, '12. 展示预约单与核销码', GREEN),
    ]
    y = 130
    for a, c, label, col in steps:
        x1, x2 = objs[a][1], objs[c][1]
        b += arrow(x1, y, x2, y, label, color=col, fs=11.5)
        y += 44
    return svg('fig4-4_booking_seq', W, H, b)


# ============ 图10 预约活动图 ============
def fig_booking_activity():
    W, H = 900, 1000
    b = ''
    cx = 450
    b += f'<circle cx="{cx}" cy="40" r="16" fill="{LINE}"/>\n'
    nodes = [
        ('用户进入课程详情页', BLUE_L, BLUE),
        ('校验登录状态', GREEN_L, GREEN),
        ('读取 booking_config 规则', GREEN_L, GREEN),
        ('名额充足且未重复预约？', ORANGE_L, ORANGE),
        ('提交预约（状态：待审核）', BLUE_L, BLUE),
        ('匠人审核', ORANGE_L, ORANGE),
        ('审核通过（状态：已通过）', GREEN_L, GREEN),
        ('生成核销码 + 发送站内消息', GREEN_L, GREEN),
        ('开课现场核销签到', PURPLE_L, PURPLE),
        ('课程结束（状态：已完成）', GREEN_L, GREEN),
        ('提交课程评价', PURPLE_L, PURPLE),
    ]
    y = 90
    for i, (t, cl, c) in enumerate(nodes):
        h = 52
        if '？' in t:
            b += box(cx - 180, y, 360, h, t, fill=cl, stroke=c, fs=13, rx=26)
        else:
            b += box(cx - 160, y, 320, h, t, fill=cl, stroke=c, fs=13)
        if i:
            b += arrow(cx, y - 12, cx, y - 2)
        y += h + 34
    b += f'<circle cx="{cx}" cy="{y}" r="18" fill="none" stroke="{LINE}" stroke-width="2"/><circle cx="{cx}" cy="{y}" r="11" fill="{LINE}"/>\n'
    b += arrow(cx, y - 52 + 2, cx, y - 20, '')
    # 异常分支
    b += box(700, 300, 170, 76, '名额不足 /\n重复预约', fill=RED_L, stroke=RED, fs=12)
    b += box(700, 486, 170, 76, '审核拒绝\n归还名额 + 通知', fill=RED_L, stroke=RED, fs=12)
    b += box(700, 672, 170, 76, '超时未核销\n标记爽约', fill=RED_L, stroke=RED, fs=12)
    b += arrow(cx + 180, 300 + 26, 696, 322, '否', color=RED)
    b += arrow(cx + 160, 512, 696, 512, '拒绝', color=RED)
    b += arrow(cx + 160, 698, 696, 700, '未到', color=RED)
    # 左侧取消分支
    b += box(60, 486, 170, 76, '用户取消 /\n管理员强制取消\n归还名额', fill=RED_L, stroke=RED, fs=11.5)
    b += arrow(cx - 160, 512, 234, 512, '取消', color=RED)
    return svg('fig4-5_booking_activity', W, H, b)


# ============ 图11 预约状态机图 ============
def fig_state():
    W, H = 1240, 620
    b = ''
    states = {
        '待审核': (120, 240, 170, 62, ORANGE, ORANGE_L),
        '已通过': (420, 240, 170, 62, GREEN, GREEN_L),
        '已核销': (720, 240, 170, 62, BLUE, BLUE_L),
        '已完成': (1020, 240, 170, 62, PURPLE, PURPLE_L),
        '已拒绝': (420, 440, 170, 62, RED, RED_L),
        '已取消': (720, 440, 170, 62, RED, RED_L),
    }
    b += f'<circle cx="40" cy="271" r="16" fill="{LINE}"/>\n'
    b += arrow(56, 271, 116, 271, '')
    for k, (x, y, w, h, c, cl) in states.items():
        b += box(x, y, w, h, k, fill=cl, stroke=c, fs=14.5, bold=True)
    b += arrow(290, 271, 416, 271, '匠人审核通过')
    b += arrow(590, 271, 716, 271, '现场核销签到')
    b += arrow(890, 271, 1016, 271, '课程结束')
    b += poly([(505, 302), (505, 380), (505, 436)], '审核拒绝')
    b += poly([(805, 302), (805, 400), (805, 436)], '用户取消 / 强制取消 / 超时未审')
    b += arrow(720, 502, 500, 502, '归还名额')
    b += arrow(590, 502, 590, 502, '')
    # 时间轴
    b += f'<line x1="60" y1="560" x2="1180" y2="560" stroke="{LINE}" stroke-width="1.5"/>\n'
    for x, t in [(120, 'T0 提交'), (420, 'T1 审核'), (720, 'T2 核销'), (1020, 'T3 完成')]:
        b += f'<circle cx="{x+85}" cy="560" r="6" fill="{BLUE}"/>\n'
        b += f'<text x="{x+85}" y="588" text-anchor="middle" font-size="12.5" fill="{LINE}">{t}</text>\n'
    b += title(60, 40, '预约单六态状态机（后端枚举 + 状态流转校验，禁止非法跳转）', 15)
    return svg('fig4-6_booking_state', W, H, b)


# ============ 图12 三层防超卖机制图 ============
def fig_antoversell():
    W, H = 1180, 620
    b = ''
    b += box(420, 30, 340, 52, '并发预约请求（JMeter 100 并发）', fill=GREY_L, stroke=GREY, fs=14, bold=True)
    b += arrow(590, 82, 590, 126)
    b += box(180, 130, 820, 110, '', fill='#fbfdff', stroke=BLUE, rx=10)
    b += title(200, 158, '第一层   Redis 分布式锁（SETNX + Lua 原子释放）', 13.5)
    b += box(200, 170, 380, 56, 'SET lock:booking:{courseId} {requestId}\nNX PX 10000', fill=RED_L, stroke=RED, fs=12)
    b += box(600, 170, 380, 56, '释放：Lua 脚本比对 requestId 后 DEL\n避免误删他人锁', fill=RED_L, stroke=RED, fs=12)
    b += arrow(590, 240, 590, 286)
    b += box(180, 290, 820, 110, '', fill='#fbfdff', stroke=GREEN, rx=10)
    b += title(200, 318, '第二层   数据库行锁（事务内二次校验）', 13.5)
    b += box(200, 330, 380, 56, 'SELECT ... FOR UPDATE\n锁定课程记录', fill=GREEN_L, stroke=GREEN, fs=12)
    b += box(600, 330, 380, 56, 'IF remain_quota >= n\nUPDATE course SET remain_quota = remain_quota - n', fill=GREEN_L, stroke=GREEN, fs=12)
    b += arrow(590, 400, 590, 446)
    b += box(180, 450, 820, 110, '', fill='#fbfdff', stroke=PURPLE, rx=10)
    b += title(200, 478, '第三层   数据库唯一索引兜底', 13.5)
    b += box(200, 490, 380, 56, 'UNIQUE KEY uk_user_course\n(user_id, course_id)', fill=PURPLE_L, stroke=PURPLE, fs=12)
    b += box(600, 490, 380, 56, 'INSERT booking\n重复预约直接抛唯一键冲突', fill=PURPLE_L, stroke=PURPLE, fs=12)
    b += arrow(590, 560, 590, 596)
    b += box(380, 590, 420, 0, '', fill='#fff', stroke='#fff')
    return svg('fig4-7_antoversell', W, H, b)


# ============ 图13 优惠券策略模式类图 ============
def fig_coupon_class():
    W, H = 1100, 560
    b = ''
    b += box(60, 60, 300, 60, '«interface»\nCouponStrategy', fill=BLUE, stroke=BLUE, fs=13.5, bold=True, tc='#ffffff')
    b += box(60, 120, 300, 90, '+ calculate(amount : BigDecimal,\ncoupon : Coupon) : BigDecimal\n+ supportType() : Integer', fill='#ffffff', stroke=BLUE, fs=12)
    b += box(60, 300, 300, 100, 'FullReductionStrategy\n（满减券）', fill=GREEN_L, stroke=GREEN, fs=13, bold=True)
    b += box(60, 430, 300, 100, 'DiscountStrategy\n（折扣券）', fill=GREEN_L, stroke=GREEN, fs=13, bold=True)
    b += box(520, 60, 300, 60, 'CouponStrategyFactory', fill=ORANGE, stroke=ORANGE, fs=13.5, bold=True, tc='#ffffff')
    b += box(520, 120, 300, 110, '- strategies : Map&lt;Integer, CouponStrategy&gt;\n+ getStrategy(type : Integer)\n  : CouponStrategy', fill='#ffffff', stroke=ORANGE, fs=12)
    b += box(520, 300, 300, 100, 'CouponService\n+ calcDiscount(amount, couponId)\n  : BigDecimal', fill=PURPLE_L, stroke=PURPLE, fs=13, bold=True)
    b += box(880, 60, 180, 170, 'Coupon（实体）\ntype : Integer\nthreshold : BigDecimal\namount : BigDecimal\ndiscount : BigDecimal', fill=RED_L, stroke=RED, fs=11.5)
    b += arrow(210, 236, 210, 296, 'implements', color=LINE, lw=1.3)
    b += arrow(210, 236, 210, 426, 'implements', color=LINE, lw=1.3)
    b += arrow(520, 160, 364, 160, '持有', color=LINE, lw=1.3)
    b += arrow(520, 350, 364, 350, '调用', color=LINE, lw=1.3)
    b += arrow(820, 150, 876, 150, '读取', color=LINE, lw=1.3)
    b += title(60, 40, '优惠券计算策略模式类图（消除满减/折扣的条件分支）', 15)
    return svg('fig4-8_coupon_class', W, H, b)


if __name__ == '__main__':
    figs = [fig_function_tree, fig_deploy, fig_logic, fig_seq_flow, fig_er,
            fig_package, fig_class, fig_booking_class, fig_booking_seq,
            fig_booking_activity, fig_state, fig_antoversell, fig_coupon_class]
    for f in figs:
        p = f()
        print('OK', os.path.basename(p))
