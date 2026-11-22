package com.lmf.heritage.common;

import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * <p>编码规则：5 位数字，前 2 位为模块，后 3 位为序号。
 * 1xxxx 通用 / 2xxxx 用户 / 3xxxx 商品 / 4xxxx 订单 / 5xxxx 预约 / 6xxxx 优惠券 / 7xxxx 内容</p>
 */
@Getter
public enum ErrorCode {

    /* ========== 通用 1xxxx ========== */
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(1001, "参数校验失败"),
    NOT_LOGIN(1002, "未登录或登录已过期"),
    NO_PERMISSION(1003, "没有操作权限"),
    TOKEN_INVALID(1004, "令牌无效"),
    TOKEN_EXPIRED(1005, "令牌已过期"),
    REFRESH_TOKEN_EXPIRED(1006, "登录状态已失效，请重新登录"),
    NOT_FOUND(1007, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(1008, "请求方式不正确"),
    REPEAT_SUBMIT(1009, "操作过于频繁，请稍后再试"),
    SYSTEM_ERROR(1010, "系统繁忙，请稍后再试"),

    /* ========== 用户 2xxxx ========== */
    USER_NOT_FOUND(2001, "用户不存在"),
    USERNAME_EXISTS(2002, "用户名已被注册"),
    PASSWORD_ERROR(2003, "用户名或密码错误"),
    USER_DISABLED(2004, "账号已被禁用，请联系管理员"),
    OLD_PASSWORD_ERROR(2005, "原密码不正确"),
    CRAFTSMAN_NOT_AUDITED(2006, "匠人资质尚未通过审核"),
    CRAFTSMAN_AUDITED(2007, "匠人资质已审核，无需重复提交"),
    ADDRESS_NOT_FOUND(2008, "收货地址不存在"),
    CRAFTSMAN_NOT_FOUND(2009, "匠人档案不存在"),

    /* ========== 商品 3xxxx ========== */
    GOODS_NOT_FOUND(3001, "商品不存在或已下架"),
    GOODS_OFF_SHELF(3002, "商品已下架"),
    STOCK_NOT_ENOUGH(3003, "商品库存不足"),
    CART_ITEM_NOT_FOUND(3004, "购物车中没有该商品"),
    CATEGORY_HAS_GOODS(3005, "该分类下还有商品，无法删除"),
    GOODS_AUDIT_NOT_PASSED(3006, "商品尚未通过平台审核，无法上架"),
    CATEGORY_NAME_EXISTS(3007, "分类名称已存在"),
    CATEGORY_NOT_FOUND(3008, "分类不存在"),
    CATEGORY_HAS_COURSE(3009, "该分类下还有课程，无法删除"),

    /* ========== 订单 4xxxx ========== */
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态不允许该操作"),
    ORDER_NOT_BELONG_TO_YOU(4003, "无权操作该订单"),
    COUPON_NOT_AVAILABLE(4004, "优惠券不可用或已过期"),
    ADDRESS_REQUIRED(4005, "请先选择收货地址"),
    CART_EMPTY(4006, "购物车为空，无法结算"),

    /* ========== 预约 5xxxx（题眼，错误提示要具体） ========== */
    COURSE_NOT_FOUND(5001, "课程不存在"),
    COURSE_ALREADY_ENDED(5002, "课程已结束，无法预约"),
    COURSE_NOT_STARTED_BOOKING(5003, "课程尚未开放预约"),
    BOOKING_DEADLINE_PASSED(5004, "已超过预约截止时间"),
    BOOKING_ADVANCE_NOT_ENOUGH(5005, "距开课时间太近，无法预约"),
    BOOKING_QUOTA_NOT_ENOUGH(5006, "剩余名额不足"),
    BOOKING_DUPLICATE(5007, "你已预约过该课程，请勿重复预约"),
    BOOKING_LIMIT_EXCEEDED(5008, "已达该课程的最大预约次数"),
    BOOKING_NOT_FOUND(5009, "预约记录不存在"),
    BOOKING_STATUS_ERROR(5010, "当前预约状态不允许该操作"),
    BOOKING_NOT_BELONG_TO_YOU(5011, "无权操作该预约"),
    BOOKING_CANCEL_DEADLINE_PASSED(5012, "已超过可取消时间，如需取消请联系匠人"),
    CHECKIN_CODE_INVALID(5013, "核销码不正确"),
    CHECKIN_ALREADY_DONE(5014, "该预约已核销，请勿重复核销"),
    CHECKIN_NOT_YOUR_COURSE(5015, "该预约不属于你的课程"),
    BOOKING_AUDIT_NOT_REQUIRED(5016, "该课程无需审核"),
    BOOKING_CONFIG_NOT_FOUND(5017, "预约规则配置项不存在"),
    BOOKING_QUEUE_BUSY(5018, "当前预约人数较多，请稍后重试"),
    COURSE_STATUS_ERROR(5019, "当前课程状态不允许该操作"),
    CHECKIN_TOO_EARLY(5020, "尚未到核销时间，开课前 30 分钟才可核销"),
    CHECKIN_COURSE_ENDED(5021, "课程已结束，无法核销"),

    /* ========== 优惠券 6xxxx ========== */
    COUPON_NOT_FOUND(6001, "优惠券不存在"),
    COUPON_RECEIVED_OUT(6002, "优惠券已被领完"),
    COUPON_RECEIVE_LIMIT(6003, "你已达到该券的领取上限"),
    COUPON_EXPIRED(6004, "优惠券已过期"),
    COUPON_USED(6005, "优惠券已使用"),
    COUPON_THRESHOLD_NOT_ENOUGH(6006, "订单金额未达到优惠券使用门槛"),

    /* ========== 内容 7xxxx ========== */
    ARTICLE_NOT_FOUND(7001, "资讯不存在"),
    REVIEW_NOT_FOUND(7002, "评价不存在"),
    REVIEW_ALREADY_EXISTS(7003, "你已经评价过了"),
    FILE_TYPE_NOT_SUPPORTED(7004, "只支持 jpg / png / webp 格式的图片"),
    FILE_SIZE_EXCEEDED(7005, "图片大小不能超过 5MB"),
    UPLOAD_FAILED(7006, "文件上传失败"),
    FEEDBACK_NOT_FOUND(7007, "反馈记录不存在"),
    BANNER_NOT_FOUND(7008, "轮播图不存在"),

    /* ---------------- 8xxx 管理端业务 ---------------- */
    ADMIN_CANNOT_DISABLE(8001, "管理员账号不支持禁用操作"),
    CRAFTSMAN_AUDIT_STATUS_ERROR(8002, "该匠人资质已审核，无需重复处理"),
    GOODS_AUDIT_STATUS_ERROR(8003, "该商品已审核，无需重复处理"),
    COURSE_AUDIT_STATUS_ERROR(8004, "该课程已审核，无需重复处理"),
    ADMIN_OPERATION_EMPTY(8005, "没有可执行的记录"),
    AUDIT_REASON_REQUIRED(8006, "请填写原因");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
