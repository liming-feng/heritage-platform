package com.lmf.heritage.module.user.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户导出行
 *
 * <p>刻意<b>不导出</b>密码、邮箱这两列：密码是密文但没必要外流，
 * 邮箱属于个人信息，导出到本地 Excel 后就脱离了系统的访问控制。
 * 导出功能要按「这份文件会被发到谁手里」来定列，而不是按「页面上有什么」。</p>
 *
 * @author 李明峰 23100025
 */
@Data
@Schema(description = "用户导出行")
public class UserExcelVO {

    @ExcelProperty("用户ID")
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty("用户名")
    @ColumnWidth(16)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(16)
    private String nickname;

    @ExcelProperty("手机号")
    @ColumnWidth(16)
    private String phone;

    @ExcelProperty("性别")
    @ColumnWidth(8)
    private String genderText;

    @ExcelProperty("角色")
    @ColumnWidth(10)
    private String roleText;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("真实姓名")
    @ColumnWidth(12)
    private String realName;

    @ExcelProperty("技艺门类")
    @ColumnWidth(14)
    private String craftType;

    @ExcelProperty("资质审核")
    @ColumnWidth(12)
    private String auditStatusText;

    @ExcelProperty("注册时间")
    @ColumnWidth(20)
    private String createTime;
}
