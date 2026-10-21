package com.lmf.heritage.module.user.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.user.dto.AdminUserQueryDTO;
import com.lmf.heritage.module.user.dto.UserBatchStatusDTO;
import com.lmf.heritage.module.user.dto.UserStatusDTO;
import com.lmf.heritage.module.user.vo.AdminUserVO;
import com.lmf.heritage.module.user.vo.RegisterStatVO;
import com.lmf.heritage.module.user.vo.UserExcelVO;

import java.util.List;

/**
 * 管理端 · 用户管理
 *
 * @author 李明峰 23100025
 */
public interface AdminUserService {

    /** 用户 / 匠人列表（角色、状态、资质审核状态、关键词、注册时间范围筛选） */
    PageResult<AdminUserVO> page(AdminUserQueryDTO query);

    /** 用户详情（匠人账号额外带出资质信息） */
    AdminUserVO detail(Long id);

    /** 启用 / 禁用单个账号 */
    void updateStatus(UserStatusDTO dto);

    /** 批量启用 / 禁用，返回实际生效的条数；批内存在管理员账号时整批拒绝 */
    int batchUpdateStatus(UserBatchStatusDTO dto);

    /**
     * 注册量统计
     *
     * @param days   近 N 天趋势（null 取默认 30，上限 90）
     * @param months 近 N 月趋势（null 取默认 6，上限 24）
     */
    RegisterStatVO registerStat(Integer days, Integer months);

    /** 导出用：与列表同一套筛选条件 */
    List<UserExcelVO> exportList(AdminUserQueryDTO query);
}
