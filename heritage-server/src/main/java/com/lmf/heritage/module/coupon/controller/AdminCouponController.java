package com.lmf.heritage.module.coupon.controller;

import com.alibaba.excel.EasyExcel;
import com.lmf.heritage.annotation.LoginRequired;
import com.lmf.heritage.annotation.OperationLog;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.common.Result;
import com.lmf.heritage.module.coupon.dto.AdminCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.AdminUserCouponQueryDTO;
import com.lmf.heritage.module.coupon.dto.CouponSaveDTO;
import com.lmf.heritage.module.coupon.service.AdminCouponService;
import com.lmf.heritage.module.coupon.service.CouponService;
import com.lmf.heritage.module.coupon.vo.AdminCouponVO;
import com.lmf.heritage.module.coupon.vo.AdminUserCouponVO;
import com.lmf.heritage.module.coupon.vo.CouponExcelVO;
import com.lmf.heritage.module.coupon.vo.CouponStatVO;
import com.lmf.heritage.module.coupon.vo.UserCouponExcelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端 · 优惠券运营
 *
 * <p>券种的创建与启停都打 {@code @OperationLog}：一张券的门槛写错几个数字，
 * 直接带来的就是真金白银的差额，必须能查到是谁在什么时候改的。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Tag(name = "管理端-优惠券", description = "券种增删改查 / 启停 / 领取使用统计 / 领取记录 / 导出 / 定时任务")
@RestController
@RequestMapping("/admin/coupon")
@RequiredArgsConstructor
@LoginRequired(roles = {Constants.ROLE_ADMIN})
public class AdminCouponController {

    private final AdminCouponService adminCouponService;
    private final CouponService couponService;

    @Operation(summary = "券种列表",
            description = "含停用、未开始、已结束的券（领券中心只看得到「启用且进行中」的）。"
                    + "每项带已领取、已使用、领取率、核销率、有效期状态；"
                    + "effective 1未开始 2进行中 3已结束")
    @GetMapping("/list")
    public Result<PageResult<AdminCouponVO>> list(AdminCouponQueryDTO query) {
        return Result.success(adminCouponService.page(query));
    }

    @Operation(summary = "券种详情", description = "含领取量 / 使用量 / 核销率，以及实际领取记录数（可与已领取数对账）")
    @GetMapping("/detail")
    public Result<AdminCouponVO> detail(@RequestParam("id") Long id) {
        return Result.success(adminCouponService.detail(id));
    }

    @Operation(summary = "创建 / 编辑券种",
            description = "不传 id 为新增；传 id 为编辑。"
                    + "满减券 amount 必填、折扣券 discount 必填且取值 (0,1]；"
                    + "发放总量只能往上调（低于已领取数会返回 1001），需要收紧请用「停用」；"
                    + "结束时间早于开始时间返回 1001")
    @OperationLog("优惠券保存")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid CouponSaveDTO dto) {
        return Result.success(adminCouponService.save(dto));
    }

    @Operation(summary = "启用 / 停用券种",
            description = "status 1启用 0停用。停用后该券立即从领券中心消失，已领到手的券不受影响")
    @OperationLog("优惠券启停")
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestParam("id") Long id,
                                    @RequestParam("status") Integer status) {
        adminCouponService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "优惠券运营总览",
            description = "券种数、启用数、发放总量、累计领取、未使用/已使用/已过期、"
                    + "领取率与核销率、按类型分组统计、核销率 Top N 券种。"
                    + "核销率口径 = 已使用 / 已领取（不是 / 发放总量）")
    @GetMapping("/stat")
    public Result<CouponStatVO> stat(@RequestParam(value = "topN", required = false) Integer topN) {
        return Result.success(adminCouponService.stat(topN));
    }

    @Operation(summary = "领取记录",
            description = "可按 couponId（这张券被谁领了）、userId、status、关键词（用户名/昵称/券名/订单号）、"
                    + "领取时间范围筛选")
    @GetMapping("/receive-list")
    public Result<PageResult<AdminUserCouponVO>> receiveList(AdminUserCouponQueryDTO query) {
        return Result.success(adminCouponService.receivePage(query));
    }

    @Operation(summary = "导出券种 Excel",
            description = "筛选条件与券种列表一致，最多 5000 条。数量与比率导出为数值型，可直接排序")
    @GetMapping("/export")
    public void export(AdminCouponQueryDTO query, HttpServletResponse response) throws IOException {
        List<CouponExcelVO> rows = adminCouponService.exportList(query);
        write(response, "优惠券列表", rows, CouponExcelVO.class, "优惠券列表");
        log.info("[优惠券管理] 导出券种 {} 条", rows.size());
    }

    @Operation(summary = "导出领取记录 Excel",
            description = "筛选条件与领取记录一致，最多 5000 条。刻意不导出手机号——"
                    + "用户名与订单号已足够追溯到具体订单")
    @GetMapping("/receive-export")
    public void exportReceive(AdminUserCouponQueryDTO query, HttpServletResponse response) throws IOException {
        List<UserCouponExcelVO> rows = adminCouponService.exportReceiveList(query);
        write(response, "优惠券领取记录", rows, UserCouponExcelVO.class, "领取记录");
        log.info("[优惠券管理] 导出领取记录 {} 条", rows.size());
    }

    /* ==================== 定时任务手动触发（联调与演示用） ==================== */

    @Operation(summary = "手动触发：标记过期券",
            description = "把过了有效期且未使用的券置为「已过期」；重复触发不产生副作用")
    @PostMapping("/task/mark-expired")
    public Result<Integer> markExpired() {
        return Result.success(couponService.markExpired());
    }

    @Operation(summary = "手动触发：优惠券过期提醒",
            description = "给 24 小时内过期的券发站内消息；同一张券只提醒一次（靠 message 表去重）")
    @PostMapping("/task/expire-reminders")
    public Result<Integer> expireReminders() {
        return Result.success(couponService.sendExpireReminders());
    }

    /** 统一的 Excel 写出：三个导出接口共用，避免文件名编码各写一遍 */
    private static <T> void write(HttpServletResponse response, String fileName,
                                  List<T> rows, Class<T> clazz, String sheetName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(rows);
    }
}
