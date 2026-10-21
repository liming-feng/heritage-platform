package com.lmf.heritage.module.booking.enums;

import com.lmf.heritage.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预约状态机单元测试（题眼）
 *
 * <p>状态流转表（与 BookingStatusEnum.TRANSITIONS 一一对应）：</p>
 * <pre>
 * 待审核(0) → 已通过(1) / 已拒绝(2) / 已取消(3)
 * 已通过(1) → 已核销(4) / 已取消(3)
 * 已核销(4) → 已完成(5)
 * 已拒绝(2) / 已取消(3) / 已完成(5) → 终态
 * </pre>
 *
 * <p>验收标准里特别要求「含预约状态机非法跳转的用例」，所以非法路径是本类的重点。</p>
 */
class BookingStatusEnumTest {

    /* ============ 合法流转 ============ */

    @ParameterizedTest(name = "[{index}] {0} -> {1} 应当允许")
    @CsvSource({
            "0, 1",  // 待审核 → 已通过（匠人审核通过）
            "0, 2",  // 待审核 → 已拒绝（匠人拒绝 / 超时自动拒绝）
            "0, 3",  // 待审核 → 已取消（用户取消）
            "1, 4",  // 已通过 → 已核销（开课核销）
            "1, 3",  // 已通过 → 已取消（用户/匠人/管理员取消）
            "4, 5"   // 已核销 → 已完成（课程结束）
    })
    @DisplayName("流转表内的所有合法路径都能通过")
    void legalTransfers(int from, int to) {
        assertDoesNotThrow(() ->
                BookingStatusEnum.checkTransfer(from, BookingStatusEnum.of(to)));
        assertTrue(BookingStatusEnum.of(from).canTransferTo(BookingStatusEnum.of(to)));
    }

    /* ============ 非法流转（验收重点） ============ */

    @ParameterizedTest(name = "[{index}] {0} -> {1} 必须被拒绝")
    @CsvSource({
            "0, 4",  // 待审核不能直接核销（必须先审核通过）
            "0, 5",  // 待审核不能直接完成
            "1, 2",  // 已通过不能改判为拒绝（要取消走取消，且名额归还口径不同）
            "1, 5",  // 已通过不能跳过核销直接完成（未到场 = 爽约，不算完成）
            "2, 1",  // 已拒绝是终态，不能复活为已通过
            "2, 3",  // 已拒绝不能流转为已取消
            "3, 1",  // 已取消不能直接复活为已通过（重新预约走「复活记录」新流程）
            "3, 4",  // 已取消不能核销
            "4, 3",  // 已核销不能取消（人已经到场了，名额已消耗）
            "4, 2",  // 已核销不能变成拒绝
            "5, 4",  // 已完成不能回退
            "5, 3"   // 已完成是终态
    })
    @DisplayName("所有非法跳转都抛业务异常，且错误码为 5003")
    void illegalTransfers(int from, int to) {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStatusEnum.checkTransfer(from, BookingStatusEnum.of(to)));
        assertEquals(5010, ex.getCode(), "非法流转应返回 BOOKING_STATUS_ERROR(5010)");
    }

    @Test
    @DisplayName("非法流转的提示包含当前状态名，方便用户理解")
    void illegalTransferMessage() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                BookingStatusEnum.checkTransfer(3, BookingStatusEnum.PASSED));
        assertTrue(ex.getMessage().contains("已取消"),
                "提示应带上具体状态名「已取消」，实际：" + ex.getMessage());
    }

    /* ============ 终态判定 ============ */

    @Test
    @DisplayName("已拒绝/已取消/已完成是终态；待审核/已通过/已核销不是")
    void finalStates() {
        assertTrue(BookingStatusEnum.REJECTED.isFinal());
        assertTrue(BookingStatusEnum.CANCELED.isFinal());
        assertTrue(BookingStatusEnum.FINISHED.isFinal());
        assertFalse(BookingStatusEnum.WAIT_AUDIT.isFinal());
        assertFalse(BookingStatusEnum.PASSED.isFinal());
        assertFalse(BookingStatusEnum.CHECKED_IN.isFinal());
    }

    /* ============ 名额占用口径（与防超卖、限约次数共用） ============ */

    @Test
    @DisplayName("占名额状态 = {待审核, 已通过, 已核销, 已完成}；已拒绝/已取消归还名额")
    void quotaOccupied() {
        assertTrue(BookingStatusEnum.WAIT_AUDIT.isQuotaOccupied());
        assertTrue(BookingStatusEnum.PASSED.isQuotaOccupied());
        assertTrue(BookingStatusEnum.CHECKED_IN.isQuotaOccupied());
        assertTrue(BookingStatusEnum.FINISHED.isQuotaOccupied());
        assertFalse(BookingStatusEnum.REJECTED.isQuotaOccupied(), "已拒绝应归还名额");
        assertFalse(BookingStatusEnum.CANCELED.isQuotaOccupied(), "已取消应归还名额");
    }

    /* ============ code ↔ 枚举互转 ============ */

    @Test
    @DisplayName("of(null) 与 of(未知 code) 都抛业务异常")
    void ofEdge() {
        assertThrows(BusinessException.class, () -> BookingStatusEnum.of(null));
        assertThrows(BusinessException.class, () -> BookingStatusEnum.of(99));
        assertThrows(BusinessException.class, () -> BookingStatusEnum.checkTransfer(null, BookingStatusEnum.PASSED));
    }

    @Test
    @DisplayName("全部 6 个状态的 code 与描述一一对应")
    void codeMap() {
        assertEquals(0, BookingStatusEnum.WAIT_AUDIT.getCode());
        assertEquals("待审核", BookingStatusEnum.WAIT_AUDIT.getDesc());
        assertEquals(1, BookingStatusEnum.PASSED.getCode());
        assertEquals(2, BookingStatusEnum.REJECTED.getCode());
        assertEquals(3, BookingStatusEnum.CANCELED.getCode());
        assertEquals(4, BookingStatusEnum.CHECKED_IN.getCode());
        assertEquals(5, BookingStatusEnum.FINISHED.getCode());
        assertEquals(6, BookingStatusEnum.values().length);
    }
}
