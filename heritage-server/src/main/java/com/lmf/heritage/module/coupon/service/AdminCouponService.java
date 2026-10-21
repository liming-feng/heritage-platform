package com.lmf.heritage.module.coupon.service;

import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.coupon.dto.AdminCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.AdminUserCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.CouponSaveDTO;
import com.lmf.heritage.module.coupon.vo.AdminCouponVO;
import com.lmf.heritage.module.coupon.vo.AdminUserCouponVO;
import com.lmf.heritage.module.coupon.vo.CouponExcelVO;
import com.lmf.heritage.module.coupon.vo.CouponStatVO;
import com.lmf.heritage.module.coupon.vo.UserCouponExcelVO;

import java.util.List;

/**
 * 管理端 · 优惠券运营
 *
 * <p>与用户端 {@code CouponService} 分开：用户端关心「我能不能领、能用减多少」，
 * 管理端关心「这张券发出去多少、核销了多少、值不值得再发一次」。
 * 两类问题的字段与校验完全不同，放在一个接口里只会互相干扰。</p>
 *
 * @author 李明峰 23100025
 */
public interface AdminCouponService {

    /** 券种列表（含停用、未开始、已结束） */
    PageResult<AdminCouponVO> page(AdminCouponQueryDTO query);

    /** 券种详情（含领取量 / 使用量 / 核销率） */
    AdminCouponVO detail(Long id);

    /** 创建 / 编辑券种，返回券 id */
    Long save(CouponSaveDTO dto);

    /** 启用 / 停用券种 */
    void updateStatus(Long id, Integer status);

    /** 运营总览：券种数、发放量、领取量、使用量、领取率、核销率、按类型分组、核销排行 */
    CouponStatVO stat(Integer topN);

    /** 领取记录（可按券种、用户、状态、关键词、时间范围筛选） */
    PageResult<AdminUserCouponVO> receivePage(AdminUserCouponQueryDTO query);

    /** 券种列表导出 */
    List<CouponExcelVO> exportList(AdminCouponQueryDTO query);

    /** 领取记录导出 */
    List<UserCouponExcelVO> exportReceiveList(AdminUserCouponQueryDTO query);
}
