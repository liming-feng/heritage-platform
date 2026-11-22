package com.lmf.heritage.common;

/**
 * 全局常量
 */
public interface Constants {

    /* ---------------- 角色 ---------------- */
    int ROLE_USER = 1;
    int ROLE_CRAFTSMAN = 2;
    int ROLE_ADMIN = 3;

    /* ---------------- 通用状态 ---------------- */
    int STATUS_DISABLED = 0;
    int STATUS_ENABLED = 1;

    /* ---------------- Redis Key 前缀 ---------------- */
    /** 登录令牌黑名单 */
    String KEY_TOKEN_BLACKLIST = "auth:blacklist:";
    /** 预约分布式锁 */
    String KEY_BOOKING_LOCK = "lock:booking:";
    /** 商品库存分布式锁 */
    String KEY_GOODS_LOCK = "lock:goods:";
    /** 领券分布式锁（后缀 couponId:userId） */
    String KEY_COUPON_RECEIVE_LOCK = "lock:coupon:receive:";
    /** 商品浏览量计数器 */
    String KEY_GOODS_VIEW = "goods:view:";
    /** 课程浏览量计数器 */
    String KEY_COURSE_VIEW = "course:view:";
    /** 资讯浏览量计数器 */
    String KEY_ARTICLE_VIEW = "article:view:";
    /** 轮播图按位置缓存前缀（后缀为 position:limit） */
    String KEY_BANNER_POS = "banner:pos:";
    /** 热门资讯缓存（无筛选条件，可整体缓存） */
    String KEY_ARTICLE_HOT = "article:hot";
    /** 首页分类缓存 */
    String KEY_HOME_CATEGORIES = "home:categories";
    /** 热门商品缓存 */
    String KEY_HOME_HOT_GOODS = "home:hotGoods";
    /** 推荐匠人缓存 */
    String KEY_HOME_CRAFTSMEN = "home:craftsmen";
    /** 课程列表缓存 */
    String KEY_COURSE_LIST = "course:list";
    /** 预约规则配置缓存 */
    String KEY_BOOKING_CONFIG = "booking:config";
    /** 接口限流前缀 */
    String KEY_RATE_LIMIT = "rate:limit:";
    /** 评价点赞去重集合前缀 */
    String KEY_REVIEW_LIKE = "review:like:";
    /** 用户启停状态缓存（后缀 userId，值 "1"启用 / "0"禁用） */
    String KEY_USER_STATUS = "user:status:";

    /* ---------------- 缓存 TTL（秒） ---------------- */
    long TTL_BANNER = 30 * 60L;
    long TTL_CATEGORIES = 60 * 60L;
    long TTL_HOT_GOODS = 10 * 60L;
    long TTL_CRAFTSMEN = 30 * 60L;
    long TTL_COURSE_LIST = 10 * 60L;
    /** 热门资讯缓存 */
    long TTL_ARTICLE_HOT = 10 * 60L;
    /** 空结果缓存，防穿透 */
    long TTL_EMPTY = 5 * 60L;
    /** 预约规则缓存 */
    long TTL_BOOKING_CONFIG = 60 * 60L;
    /**
     * 用户启停状态缓存
     *
     * <p>禁用/启用要「立即生效」就必须在每个请求上判一次状态，而每个请求查一次库
     * 代价太大。折中方案：缓存 10 分钟，管理端改动时主动写/删这个键，
     * 于是「管理员点禁用」到「该用户下一个请求被拒」之间没有延迟，
     * 同时正常请求的命中率高到几乎不查库。</p>
     */
    long TTL_USER_STATUS = 10 * 60L;

    /* ---------------- 业务常量 ---------------- */
    /** 订单状态 */
    int ORDER_WAIT_PAY = 0;
    int ORDER_WAIT_DELIVER = 1;
    int ORDER_WAIT_RECEIVE = 2;
    int ORDER_FINISHED = 3;
    int ORDER_REFUNDED = 4;
    int ORDER_CANCELED = 5;

    /**
     * 商品状态
     *
     * <p>「待审核」是匠人发布动作的落点：发布后先进入待审核（2），
     * 管理端审核通过并置为上架（1）才对前台可见。</p>
     */
    int GOODS_OFF_SHELF = 0;
    int GOODS_ON_SHELF = 1;
    int GOODS_WAIT_AUDIT = 2;

    /** 审核状态（商品与课程共用） */
    int AUDIT_WAIT = 0;
    int AUDIT_PASSED = 1;
    int AUDIT_REJECTED = 2;

    /** 课程状态 */
    int COURSE_NOT_START = 0;
    int COURSE_ONGOING = 1;
    int COURSE_ENDED = 2;

    /** 库存预警阈值：在售商品库存小于等于该值时进入预警列表 */
    int STOCK_WARN_THRESHOLD = 5;

    /** 退款状态 */
    int REFUND_NONE = 0;
    int REFUND_APPLYING = 1;
    int REFUND_APPROVED = 2;
    int REFUND_REJECTED = 3;

    /** 预约状态（题眼状态机） */
    int BOOKING_WAIT_AUDIT = 0;
    int BOOKING_PASSED = 1;
    int BOOKING_REJECTED = 2;
    int BOOKING_CANCELED = 3;
    int BOOKING_CHECKED_IN = 4;
    int BOOKING_FINISHED = 5;

    /** 预约取消发起方 */
    int CANCEL_BY_USER = 1;
    int CANCEL_BY_CRAFTSMAN = 2;
    int CANCEL_BY_ADMIN = 3;
    int CANCEL_BY_SYSTEM = 4;

    /** 核销方式：1 扫码/输入核销码，2 报名名单里手动核销 */
    int CHECKIN_WAY_CODE = 1;
    int CHECKIN_WAY_MANUAL = 2;

    /**
     * 核销时间窗口：开课前多少分钟开放核销（窗口截止 = 课程结束时间）。
     *
     * <p>没有窗口的话「提前一周核销」也能成功，签到就失去了「本人到场」的意义，
     * 也挡不住代签。设为 30 分钟是留给学员提前到场的合理缓冲。</p>
     */
    int CHECKIN_ADVANCE_MINUTES = 30;

    /** 评价类型 */
    int REVIEW_TYPE_GOODS = 1;
    int REVIEW_TYPE_COURSE = 2;

    /** 浏览/收藏目标类型 */
    int TARGET_GOODS = 1;
    int TARGET_COURSE = 2;
    int TARGET_ARTICLE = 3;

    /** 优惠券类型：1满减 2折扣（策略模式的派发依据） */
    int COUPON_TYPE_FULL_REDUCTION = 1;
    int COUPON_TYPE_DISCOUNT = 2;

    /** 优惠券适用范围 */
    int COUPON_SCOPE_ALL = 1;
    int COUPON_SCOPE_GOODS = 2;
    int COUPON_SCOPE_COURSE = 3;

    /** 用户券状态 */
    int USER_COUPON_UNUSED = 0;
    int USER_COUPON_USED = 1;
    int USER_COUPON_EXPIRED = 2;

    /** 资讯状态 */
    int ARTICLE_OFFLINE = 0;
    int ARTICLE_PUBLISHED = 1;

    /** 留言反馈状态 */
    int FEEDBACK_WAIT = 0;
    int FEEDBACK_PROCESSING = 1;
    int FEEDBACK_DONE = 2;

    /** 站内消息类型 */
    int MSG_BOOKING_AUDIT = 1;
    int MSG_ORDER_DELIVERY = 2;
    int MSG_REFUND = 3;
    int MSG_COUPON = 4;
    int MSG_SYSTEM = 5;
    int MSG_BOOKING_REMIND = 6;

    /** 预约规则配置键 */
    String CFG_BOOKING_ADVANCE_HOURS = "booking_advance_hours";
    String CFG_CANCEL_BEFORE_HOURS = "cancel_before_hours";
    String CFG_MAX_BOOKING_PER_COURSE = "max_booking_per_course";
    String CFG_AUDIT_REQUIRED = "audit_required";
    String CFG_AUDIT_TIMEOUT_HOURS = "audit_timeout_hours";
}
