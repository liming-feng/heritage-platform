package com.lmf.heritage.module.booking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.booking.dto.BookingConfigUpdateDTO;
import com.lmf.heritage.module.booking.entity.BookingConfig;
import com.lmf.heritage.module.booking.mapper.BookingConfigMapper;
import com.lmf.heritage.module.booking.service.BookingConfigService;
import com.lmf.heritage.module.booking.vo.BookingConfigVO;
import com.lmf.heritage.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预约规则配置服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingConfigServiceImpl extends ServiceImpl<BookingConfigMapper, BookingConfig>
        implements BookingConfigService {

    private final BookingConfigMapper configMapper;
    private final CacheHelper cacheHelper;

    /** 配置键 -> 语义类型 */
    private static final Map<String, String> KEY_TYPE = new LinkedHashMap<>();
    /** 配置键 -> 单位 */
    private static final Map<String, String> KEY_UNIT = new LinkedHashMap<>();
    /** 出厂默认值，用于 resetToDefault */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        KEY_TYPE.put(Constants.CFG_BOOKING_ADVANCE_HOURS, "int");
        KEY_TYPE.put(Constants.CFG_CANCEL_BEFORE_HOURS, "int");
        KEY_TYPE.put(Constants.CFG_MAX_BOOKING_PER_COURSE, "int");
        KEY_TYPE.put(Constants.CFG_AUDIT_REQUIRED, "boolean");
        KEY_TYPE.put(Constants.CFG_AUDIT_TIMEOUT_HOURS, "int");

        KEY_UNIT.put(Constants.CFG_BOOKING_ADVANCE_HOURS, "小时");
        KEY_UNIT.put(Constants.CFG_CANCEL_BEFORE_HOURS, "小时");
        KEY_UNIT.put(Constants.CFG_MAX_BOOKING_PER_COURSE, "次");
        KEY_UNIT.put(Constants.CFG_AUDIT_REQUIRED, "");
        KEY_UNIT.put(Constants.CFG_AUDIT_TIMEOUT_HOURS, "小时");

        DEFAULTS.put(Constants.CFG_BOOKING_ADVANCE_HOURS, "2");
        DEFAULTS.put(Constants.CFG_CANCEL_BEFORE_HOURS, "24");
        DEFAULTS.put(Constants.CFG_MAX_BOOKING_PER_COURSE, "1");
        DEFAULTS.put(Constants.CFG_AUDIT_REQUIRED, "1");
        DEFAULTS.put(Constants.CFG_AUDIT_TIMEOUT_HOURS, "24");
    }

    /* ==================== 读配置（带缓存） ==================== */

    /**
     * 读取全部配置并转成 Map
     *
     * <p>缓存整体一份而不是逐键缓存：配置项一共 5 条、读写频率极低，
     * 整份缓存 + 更新时整体删除最简单也最不容易出错。</p>
     */
    private Map<String, String> loadConfig() {
        List<BookingConfig> list = cacheHelper.getListOrLoad(
                Constants.KEY_BOOKING_CONFIG, BookingConfig.class, Constants.TTL_BOOKING_CONFIG,
                () -> configMapper.selectList(new LambdaQueryWrapper<BookingConfig>().orderByAsc(BookingConfig::getId)));

        Map<String, String> map = new LinkedHashMap<>();
        for (BookingConfig c : list) {
            if (StringUtils.hasText(c.getConfigKey())) {
                map.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        return map;
    }

    @Override
    public String getString(String key, String defaultValue) {
        String v = loadConfig().get(key);
        return StringUtils.hasText(v) ? v : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = loadConfig().get(key);
        if (!StringUtils.hasText(v)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            // 库里被人改成非法值时不让整个业务崩掉，回落到默认值并在日志里留痕
            log.warn("[预约配置] 配置项 {} 的值「{}」不是合法整数，已回落到默认值 {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String v = loadConfig().get(key);
        if (!StringUtils.hasText(v)) {
            return defaultValue;
        }
        String s = v.trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
            return false;
        }
        return defaultValue;
    }

    /* ==================== 管理端 ==================== */

    @Override
    public List<BookingConfigVO> listAll() {
        List<BookingConfig> list = configMapper.selectList(
                new LambdaQueryWrapper<BookingConfig>().orderByAsc(BookingConfig::getId));
        List<BookingConfigVO> result = new ArrayList<>(list.size());
        for (BookingConfig c : list) {
            BookingConfigVO vo = new BookingConfigVO();
            vo.setId(c.getId());
            vo.setConfigKey(c.getConfigKey());
            vo.setConfigValue(c.getConfigValue());
            vo.setConfigName(c.getConfigName());
            vo.setDescription(c.getDescription());
            vo.setUpdateTime(c.getUpdateTime());
            vo.setValueType(KEY_TYPE.getOrDefault(c.getConfigKey(), "string"));
            vo.setUnit(KEY_UNIT.getOrDefault(c.getConfigKey(), ""));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BookingConfigUpdateDTO dto) {
        BookingConfig config = configMapper.selectOne(new LambdaQueryWrapper<BookingConfig>()
                .eq(BookingConfig::getConfigKey, dto.getConfigKey()).last("LIMIT 1"));
        BusinessException.throwIf(config == null, ErrorCode.BOOKING_CONFIG_NOT_FOUND,
                "预约规则「" + dto.getConfigKey() + "」不存在");

        String value = dto.getConfigValue().trim();
        validateValue(dto.getConfigKey(), value, config.getConfigName());

        BookingConfig update = new BookingConfig();
        update.setId(config.getId());
        update.setConfigValue(value);
        configMapper.updateById(update);

        // 先更新数据库、再删缓存：下一次读取会回源拿到新值
        evictCache();
        log.info("[预约配置] {} 由「{}」改为「{}」", dto.getConfigKey(), config.getConfigValue(), value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetToDefault() {
        int count = 0;
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            BookingConfig config = configMapper.selectOne(new LambdaQueryWrapper<BookingConfig>()
                    .eq(BookingConfig::getConfigKey, e.getKey()).last("LIMIT 1"));
            if (config == null || e.getValue().equals(config.getConfigValue())) {
                continue;
            }
            BookingConfig update = new BookingConfig();
            update.setId(config.getId());
            update.setConfigValue(e.getValue());
            configMapper.updateById(update);
            count++;
        }
        evictCache();
        log.info("[预约配置] 已恢复默认值，共变更 {} 项", count);
        return count;
    }

    @Override
    public void evictCache() {
        cacheHelper.delete(Constants.KEY_BOOKING_CONFIG);
    }

    /* ==================== 内部方法 ==================== */

    /**
     * 按配置键的语义校验取值
     *
     * <p>把「哪个键允许什么范围」集中在这里，而不是散落在调用处：
     * 管理端改错一个值，用户端提交预约时才炸，这种问题很难查。</p>
     */
    private void validateValue(String key, String value, String configName) {
        String label = StringUtils.hasText(configName) ? configName : key;
        switch (key) {
            case Constants.CFG_AUDIT_REQUIRED -> {
                if (!"0".equals(value) && !"1".equals(value)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, label + "只能是 0（免审核）或 1（需审核）");
                }
            }
            case Constants.CFG_AUDIT_TIMEOUT_HOURS -> {
                int v = parseIntOrThrow(value, label);
                // 允许 0：表示「提交即视为超时」，用于现场演示与自动化自测时快速验证超时自动拒绝链路
                if (v < 0 || v > 720) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, label + "需在 0 ~ 720 小时之间（0 表示立即超时）");
                }
            }
            case Constants.CFG_BOOKING_ADVANCE_HOURS, Constants.CFG_CANCEL_BEFORE_HOURS -> {
                int v = parseIntOrThrow(value, label);
                if (v < 0 || v > 720) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, label + "需在 0 ~ 720 小时之间");
                }
            }
            case Constants.CFG_MAX_BOOKING_PER_COURSE -> {
                int v = parseIntOrThrow(value, label);
                if (v < 0 || v > 10) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, label + "需在 0 ~ 10 之间");
                }
            }
            default -> {
                // 未知配置项按字符串放行，保持扩展性
            }
        }
    }

    private int parseIntOrThrow(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "必须是整数");
        }
    }
}
