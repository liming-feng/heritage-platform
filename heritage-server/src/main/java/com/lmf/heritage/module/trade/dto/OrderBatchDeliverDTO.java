package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量发货请求
 *
 * <p><b>为什么物流公司在外层、单号在内层</b>：一次批量发货通常是把一批货同时交给
 * 同一家快递，公司是批量的公共属性；但每个包裹的单号必然不同。
 * 若把公司也放进每一项，操作员要重复填 N 遍同一个名字，是最容易被填错的地方。</p>
 *
 * <p><b>为什么不做「部分成功」</b>：一旦有一单不合格（已发过货、不属于自己），
 * 整批回滚并明确告诉操作员是哪几单有问题。发货是有物理后果的动作
 * ——「有几单悄悄没发出去」比「整批重来一次」危险得多。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "批量发货请求")
public class OrderBatchDeliverDTO {

    @Size(max = 50, message = "物流公司最长 50 个字符")
    @Schema(description = "物流公司，本批次共用", example = "顺丰速运")
    private String logisticsCompany;

    @NotEmpty(message = "请至少选择一笔订单")
    @Size(max = 50, message = "单次最多发货 50 笔")
    @Valid
    @Schema(description = "待发货订单明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    /** 单笔订单的发货明细 */
    @Data
    @Schema(description = "批量发货明细项")
    public static class Item {

        @NotNull(message = "订单 id 不能为空")
        @Schema(description = "订单 id")
        private Long orderId;

        @Size(max = 50, message = "物流单号最长 50 个字符")
        @Schema(description = "物流单号", example = "SF1234567890")
        private String logisticsNo;

        @Size(max = 255, message = "备注最长 255 个字符")
        @Schema(description = "本单备注（可选，会写入匠人备注）")
        private String craftsmanRemark;
    }
}
