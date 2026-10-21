package com.lmf.heritage.module.goods.enums;

import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品状态与审核状态
 *
 * <pre>
 *   匠人发布 ──→ 待审核(2, audit=0)
 *                    │
 *        审核通过 ───┼───→ 上架(1, audit=1) ←──重新上架──┐
 *        审核驳回 ───┘                                  │
 *              ↓                                    下架(0, audit=1)
 *        待审核(2, audit=2)  ←──匠人改稿重提──────────────┘
 *
 *   管理端违规下架：(1, audit=1) ──→ (0, audit=2) + reject_reason="【违规下架】…"
 * </pre>
 *
 * <p><b>状态与审核状态为什么分成两个字段</b>：商品「是否在前台售卖」（status）与
 * 「平台是否放行」（auditStatus）是两件事。已通过审核的商品匠人可以自行下架，
 * 下架不该把审核结果一起清掉——否则重新上架又要等平台再审一遍。</p>
 *
 * <p><b>但违规下架必须把审核结果一起打回</b>：如果只把 status 改成 0，
 * 匠人点一下「上架」（准入条件正是 audit_status = 1）就能把平台的处置撤销掉。
 * 所以违规下架走 (0, audit=2)，并在 reject_reason 上打「【违规下架】」前缀 ——
 * auditStatus=2 这一个值同时承载「审核驳回」与「违规下架」两种来源，
 * 状态位分不出来，靠这句带前缀的原因自述来历。</p>
 *
 * @author 李明峰 23100025
 */
@Getter
public enum GoodsStatusEnum {

    OFF_SHELF(0, "已下架"),
    ON_SHELF(1, "已上架"),
    WAIT_AUDIT(2, "待审核");

    private final int code;
    private final String desc;

    GoodsStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<Integer, GoodsStatusEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(GoodsStatusEnum::getCode, e -> e));

    public static GoodsStatusEnum of(Integer code) {
        GoodsStatusEnum status = code == null ? null : CODE_MAP.get(code);
        if (status == null) {
            throw new BusinessException(ErrorCode.GOODS_NOT_FOUND, "商品状态异常");
        }
        return status;
    }

    public static String descOf(Integer code) {
        GoodsStatusEnum e = code == null ? null : CODE_MAP.get(code);
        return e == null ? "" : e.getDesc();
    }

    /**
     * 商品审核状态（0待审核 1通过 2驳回）
     *
     * <p>与商品状态同处一个枚举文件：两者总是一起被判断，
     * 分开成两个文件反而要在每个使用点 import 两次。</p>
     */
    @Getter
    public enum Audit {

        WAIT(0, "待审核"),
        PASSED(1, "已通过"),
        REJECTED(2, "已驳回");

        private final int code;
        private final String desc;

        Audit(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        private static final Map<Integer, Audit> MAP = Arrays.stream(values())
                .collect(Collectors.toMap(Audit::getCode, e -> e));

        public static String descOf(Integer code) {
            Audit e = code == null ? null : MAP.get(code);
            return e == null ? "" : e.getDesc();
        }
    }
}
