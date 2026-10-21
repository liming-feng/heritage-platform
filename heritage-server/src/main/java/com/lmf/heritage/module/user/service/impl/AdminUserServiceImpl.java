package com.lmf.heritage.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.user.dto.AdminUserQueryDTO;
import com.lmf.heritage.module.user.dto.UserBatchStatusDTO;
import com.lmf.heritage.module.user.dto.UserStatusDTO;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import com.lmf.heritage.module.user.service.AdminUserService;
import com.lmf.heritage.module.user.vo.AdminUserVO;
import com.lmf.heritage.module.user.vo.RegisterStatVO;
import com.lmf.heritage.module.user.vo.UserExcelVO;
import com.lmf.heritage.util.UserStatusHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端 · 用户管理实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_TEXT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_TREND_DAYS = 90;
    private static final int DEFAULT_TREND_MONTHS = 6;
    private static final int MAX_TREND_MONTHS = 24;

    /** 导出上限：与日志导出同一个理由，列表可能很长，一次导出不能拖垮内存 */
    private static final int EXPORT_LIMIT = 5000;

    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final UserMapper userMapper;
    private final CraftsmanMapper craftsmanMapper;
    private final UserStatusHelper userStatusHelper;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<AdminUserVO> page(AdminUserQueryDTO query) {
        query.normalize();
        IPage<AdminUserVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /**
     * 查询 + 补齐展示字段（分页与导出共用这一个入口）
     *
     * <p>角色、性别、账号状态这些文案由 {@link #decorate} 在 Java 端回填，不在 SQL 里算。
     * 导出若另走一条「只查不补」的路，Excel 里就会出现整列空白——而文件打得开、行数也对，
     * 光看导出结果很难发现。绑成一个方法，两条路就没有分叉的可能。</p>
     */
    private IPage<AdminUserVO> queryPage(AdminUserQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminUserVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminUserVO> result = userMapper.selectAdminUserPage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminUserVO detail(Long id) {
        User user = userMapper.selectById(id);
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        // 匠人账号把资质信息一并带出：管理端点开一条匠人，想看的就是「他提交了什么」
        return toVO(user, loadCraftsman(user.getId()));
    }

    /* ==================== 启停 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(UserStatusDTO dto) {
        User user = userMapper.selectById(dto.getId());
        BusinessException.throwIf(user == null, ErrorCode.USER_NOT_FOUND);
        assertNotAdmin(user);

        User update = new User();
        update.setId(user.getId());
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);

        // 状态缓存必须同步刷新 —— 否则「禁用」要等 10 分钟缓存过期才生效，
        // 而 P8 的验收标准里恰好有一条是「修改后立即生效」的同类要求
        userStatusHelper.refresh(user.getId(), dto.getStatus());
        log.info("[用户管理] {} 账号 #{}（{}）", dto.getStatus() == 1 ? "启用" : "禁用",
                user.getId(), user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(UserBatchStatusDTO dto) {
        List<Long> ids = dto.getIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        BusinessException.throwIf(ids.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);

        List<User> users = userMapper.selectBatchIds(ids);
        BusinessException.throwIf(users.isEmpty(), ErrorCode.ADMIN_OPERATION_EMPTY);

        // 整批回滚语义（与匠人端「批量发货」保持一致）：
        // 只要批里混进了管理员账号，这一批就整体拒绝，不搞「跳过有问题的、改掉其余的」——
        // 后者会让操作者以为全改成功了，而实际上只改了一部分
        List<String> adminNames = users.stream()
                .filter(u -> Integer.valueOf(Constants.ROLE_ADMIN).equals(u.getRole()))
                .map(User::getUsername)
                .toList();
        BusinessException.throwIf(!adminNames.isEmpty(), ErrorCode.ADMIN_CANNOT_DISABLE,
                "批量操作中包含管理员账号：" + String.join("、", adminNames));

        int changed = 0;
        for (User user : users) {
            if (Integer.valueOf(dto.getStatus()).equals(user.getStatus())) {
                continue;
            }
            User update = new User();
            update.setId(user.getId());
            update.setStatus(dto.getStatus());
            changed += userMapper.updateById(update);
            userStatusHelper.refresh(user.getId(), dto.getStatus());
        }
        log.info("[用户管理] 批量{} {} 个账号", dto.getStatus() == 1 ? "启用" : "禁用", changed);
        return changed;
    }

    /* ==================== 注册量统计 ==================== */

    @Override
    public RegisterStatVO registerStat(Integer days, Integer months) {
        int d = clamp(days, 1, MAX_TREND_DAYS, DEFAULT_TREND_DAYS);
        int m = clamp(months, 1, MAX_TREND_MONTHS, DEFAULT_TREND_MONTHS);

        RegisterStatVO vo = new RegisterStatVO();
        vo.setDays(d);
        vo.setMonths(m);
        vo.setTotal(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        vo.setTodayNew(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, LocalDate.now().atStartOfDay())));
        vo.setWeekNew(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, LocalDate.now().minusDays(6).atStartOfDay())));
        vo.setMonthNew(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, LocalDate.now().withDayOfMonth(1).atStartOfDay())));

        vo.setCraftsmanTotal(craftsmanMapper.selectCount(new LambdaQueryWrapper<Craftsman>()));
        vo.setCraftsmanPassed(craftsmanMapper.selectCount(new LambdaQueryWrapper<Craftsman>()
                .eq(Craftsman::getAuditStatus, Constants.AUDIT_PASSED)));
        vo.setCraftsmanWaitAudit(craftsmanMapper.selectCount(new LambdaQueryWrapper<Craftsman>()
                .eq(Craftsman::getAuditStatus, Constants.AUDIT_WAIT)));

        vo.setDayTrend(buildDayTrend(d));
        vo.setMonthTrend(buildMonthTrend(m));
        vo.setRoleCount(buildRoleCount());
        return vo;
    }

    /**
     * 近 N 天注册趋势（日期连续，无注册的日子补 0）
     *
     * <p>右边界取「明天 0 点」而不是「今天 23:59:59」：注册时间带时分秒，
     * 用后者会漏掉今天最后一秒里注册的人。左闭右开是个不必反复思考的选择。</p>
     */
    private List<RegisterStatVO.DayPoint> buildDayTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);

        List<Map<String, Object>> raw = userMapper.selectRegisterDayTrend(start.atStartOfDay());
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : raw) {
            byDay.put(String.valueOf(row.get("statDay")), num(row.get("cnt")));
        }

        List<RegisterStatVO.DayPoint> list = new ArrayList<>(days);
        for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
            String key = cur.format(DAY);
            Long cnt = byDay.getOrDefault(key, 0L);
            RegisterStatVO.DayPoint p = new RegisterStatVO.DayPoint();
            p.setDate(key);
            p.setDateText(cur.format(DAY_TEXT));
            p.setWeekday(WEEK_CN[cur.getDayOfWeek().getValue() - 1]);
            p.setCount(cnt);
            p.setHasData(cnt > 0);
            list.add(p);
        }
        return list;
    }

    /** 近 N 月注册趋势（月份连续，无注册的月份补 0） */
    private List<RegisterStatVO.MonthPoint> buildMonthTrend(int months) {
        LocalDate thisMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate startMonth = thisMonth.minusMonths(months - 1L);

        List<Map<String, Object>> raw = userMapper.selectRegisterMonthTrend(startMonth.atStartOfDay());
        Map<String, Long> byMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : raw) {
            byMonth.put(String.valueOf(row.get("statMonth")), num(row.get("cnt")));
        }

        List<RegisterStatVO.MonthPoint> list = new ArrayList<>(months);
        for (LocalDate cur = startMonth; !cur.isAfter(thisMonth); cur = cur.plusMonths(1)) {
            String key = cur.format(MONTH);
            RegisterStatVO.MonthPoint p = new RegisterStatVO.MonthPoint();
            p.setMonth(key);
            p.setCount(byMonth.getOrDefault(key, 0L));
            list.add(p);
        }
        return list;
    }

    /**
     * 角色分布
     *
     * <p>三种角色<b>都返回</b>，没有的一个返回 0：前端饼图不需要判断空档，
     * 而且「匠人 0 人」和「这一项不存在」在业务上不是一回事。</p>
     */
    private List<RegisterStatVO.RoleCount> buildRoleCount() {
        Map<Integer, Long> byRole = new LinkedHashMap<>();
        for (Map<String, Object> row : userMapper.selectRoleCounts()) {
            byRole.put(intOf(row.get("role")), num(row.get("cnt")));
        }
        List<RegisterStatVO.RoleCount> list = new ArrayList<>(3);
        for (int role : new int[]{Constants.ROLE_USER, Constants.ROLE_CRAFTSMAN, Constants.ROLE_ADMIN}) {
            RegisterStatVO.RoleCount rc = new RegisterStatVO.RoleCount();
            rc.setRole(role);
            rc.setRoleText(roleText(role));
            rc.setCount(byRole.getOrDefault(role, 0L));
            list.add(rc);
        }
        return list;
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<UserExcelVO> exportList(AdminUserQueryDTO query) {
        query.normalize();
        // 导出与列表共用同一个 Mapper 方法，只是把分页放到最大
        // ——「导出的就是筛选后的那一批」因此是结构上成立的
        // 与分页共用 queryPage：文案字段一并补齐，导出的就是页面上看到的那一份
        List<AdminUserVO> rows = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return rows.stream().map(v -> {
            UserExcelVO e = new UserExcelVO();
            e.setId(v.getId());
            e.setUsername(v.getUsername());
            e.setNickname(v.getNickname());
            e.setPhone(v.getPhone());
            e.setGenderText(v.getGenderText());
            e.setRoleText(v.getRoleText());
            e.setStatusText(v.getStatusText());
            e.setRealName(v.getRealName());
            e.setCraftType(v.getCraftType());
            e.setAuditStatusText(v.getAuditStatusText());
            e.setCreateTime(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    /** 管理员账号是平台的运营主体，禁用后没有任何角色能把它解回来（不存在超级管理员） */
    private void assertNotAdmin(User user) {
        BusinessException.throwIf(Integer.valueOf(Constants.ROLE_ADMIN).equals(user.getRole()),
                ErrorCode.ADMIN_CANNOT_DISABLE, "管理员账号不支持禁用操作（避免平台无人可管）");
    }

    private Craftsman loadCraftsman(Long userId) {
        return craftsmanMapper.selectOne(new LambdaQueryWrapper<Craftsman>()
                .eq(Craftsman::getUserId, userId).last("LIMIT 1"));
    }

    private AdminUserVO toVO(User u, Craftsman cr) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setGender(u.getGender());
        vo.setGenderText(genderText(u.getGender()));
        vo.setRole(u.getRole());
        vo.setRoleText(roleText(u.getRole()));
        vo.setStatus(u.getStatus());
        vo.setStatusText(Integer.valueOf(Constants.STATUS_ENABLED).equals(u.getStatus()) ? "正常" : "禁用");
        vo.setCreateTime(u.getCreateTime());
        vo.setUpdateTime(u.getUpdateTime());
        if (cr != null) {
            vo.setCraftsmanId(cr.getId());
            vo.setRealName(cr.getRealName());
            vo.setCraftType(cr.getCraftType());
            vo.setAuditStatus(cr.getAuditStatus());
            vo.setAuditStatusText(auditText(cr.getAuditStatus()));
            vo.setRejectReason(cr.getRejectReason());
            vo.setFollowerCount(cr.getFollowerCount());
        }
        return vo;
    }

    private void decorate(AdminUserVO vo) {
        vo.setGenderText(genderText(vo.getGender()));
        vo.setRoleText(roleText(vo.getRole()));
        vo.setStatusText(Integer.valueOf(Constants.STATUS_ENABLED).equals(vo.getStatus()) ? "正常" : "禁用");
        if (vo.getAuditStatus() != null) {
            vo.setAuditStatusText(auditText(vo.getAuditStatus()));
        }
    }

    private static String genderText(Integer g) {
        if (g == null) {
            return "未知";
        }
        return switch (g) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    static String roleText(Integer role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case Constants.ROLE_USER -> "普通用户";
            case Constants.ROLE_CRAFTSMAN -> "匠人";
            case Constants.ROLE_ADMIN -> "管理员";
            default -> "未知";
        };
    }

    static String auditText(Integer s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case Constants.AUDIT_WAIT -> "待审核";
            case Constants.AUDIT_PASSED -> "已通过";
            case Constants.AUDIT_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private static int clamp(Integer v, int min, int max, int def) {
        if (v == null) {
            return def;
        }
        return Math.max(min, Math.min(max, v));
    }

    /** MySQL 的 COUNT(*) 走 JDBC 可能回到 Long / BigInteger / String，一律收敛成 Long */
    private static Long num(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Integer intOf(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
