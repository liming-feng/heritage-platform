package com.lmf.heritage.module.trade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增 / 修改请求
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "收货地址请求")
public class AddressDTO {

    @Schema(description = "地址 id，新增时不传")
    private Long id;

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人最长 50 个字符")
    @Schema(description = "收货人", example = "李明峰")
    private String receiver;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "联系电话", example = "13800000009")
    private String phone;

    @Size(max = 50, message = "省份最长 50 个字符")
    @Schema(description = "省", example = "黑龙江省")
    private String province;

    @Size(max = 50, message = "城市最长 50 个字符")
    @Schema(description = "市", example = "哈尔滨市")
    private String city;

    @Size(max = 50, message = "区县最长 50 个字符")
    @Schema(description = "区/县", example = "南岗区")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址最长 255 个字符")
    @Schema(description = "详细地址", example = "某某街道 123 号")
    private String detail;

    @Schema(description = "是否设为默认地址 0否 1是")
    private Integer isDefault;
}
