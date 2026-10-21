package com.lmf.heritage.module.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端 · 资讯详情（含正文）
 *
 * <p>继承列表项再补一个 {@code content}：详情页要用到的字段与列表完全一致，
 * 只多正文。用继承而不是复制一份，是为了避免「列表加了个字段、详情忘了加」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端资讯详情")
public class AdminArticleDetailVO extends AdminArticleVO {

    @Schema(description = "正文（富文本 HTML）")
    private String content;
}
