package com.lmf.heritage.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 浏览历史按日期分组（P3 验收点：列表按日期分组返回）
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "浏览历史分组")
public class HistoryGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期，yyyy-MM-dd")
    private String date;

    @Schema(description = "友好标签：今天 / 昨天 / 具体日期")
    private String label;

    @Schema(description = "该日期下的浏览记录，按浏览时间倒序")
    private List<HistoryItemVO> items;
}
