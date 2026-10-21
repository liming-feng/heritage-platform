package com.lmf.heritage.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 待办提醒
 *
 * <p>看板上「需要我现在就动手」的数字。前 3 项是 P7 任务书明确要求的，
 * 后 2 项是同一批查询顺手能拿到、且匠人确实每天要看的，一并给出来：</p>
 * <table border="1">
 *   <caption>各项的判定条件</caption>
 *   <tr><th>字段</th><th>判定条件</th></tr>
 *   <tr><td>待发货订单</td><td>本匠人的订单中 {@code status = 1}</td></tr>
 *   <tr><td>待退款申请</td><td>本匠人的订单中 {@code refund_status = 1}（申请中）</td></tr>
 *   <tr><td>待审核预约</td><td>本匠人课程的预约中 {@code status = 0}</td></tr>
 *   <tr><td>库存预警商品</td><td>在售 + 审核通过 + 库存 ≤ 阈值（与商品管理的库存预警同一口径）</td></tr>
 *   <tr><td>待回复评价</td><td>自己商品 / 课程收到、且 {@code reply} 为空的评价</td></tr>
 * </table>
 *
 * <p>每一项都与对应的列表接口使用<b>同一个过滤条件</b>，因此
 * 「待办数字 = 点进去列表的总条数」这条验收标准是天然成立的，不靠人工对齐。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "待办提醒")
public class TodoRemindVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "待发货订单数")
    private Long waitDeliverOrders;

    @Schema(description = "待审核预约数")
    private Long waitAuditBookings;

    @Schema(description = "待退款申请数（退款申请中）")
    private Long applyingRefunds;

    @Schema(description = "库存预警商品数（在售 + 审核通过 + 库存 ≤ 5）")
    private Long stockWarnGoods;

    @Schema(description = "待回复评价数")
    private Long unrepliedReviews;

    @Schema(description = "待办合计，用于侧边栏红点")
    private Long total;
}
