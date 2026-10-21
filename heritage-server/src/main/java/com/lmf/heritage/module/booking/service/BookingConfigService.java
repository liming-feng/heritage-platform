package com.lmf.heritage.module.booking.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.booking.dto.BookingConfigUpdateDTO;
import com.lmf.heritage.module.booking.entity.BookingConfig;
import com.lmf.heritage.module.booking.vo.BookingConfigVO;

import java.util.List;

/**
 * 预约规则配置服务（配置驱动，不写死常量）
 *
 * <p>规则本身是数据：管理端改一条记录，下一次提交 / 取消预约立刻按新规则走，不用重启。
 * 读路径全部带上 Redis 缓存（{@code booking:config}），写路径在更新后<b>删缓存</b>——
 * 用「先更新数据库、再删缓存」而不是「更新缓存」，避免两个并发写把缓存写成旧值。</p>
 *
 * @author 李明峰 23100025
 */
public interface BookingConfigService extends IService<BookingConfig> {

    /** 取字符串配置，不存在时返回默认值 */
    String getString(String key, String defaultValue);

    /** 取整数配置，值非法时回落到默认值并记警告 */
    int getInt(String key, int defaultValue);

    /** 取布尔配置（值为 "1"/"true" 视为真） */
    boolean getBoolean(String key, boolean defaultValue);

    /** 管理端：全部配置项（带语义类型与单位，便于前端渲染表单） */
    List<BookingConfigVO> listAll();

    /**
     * 管理端：修改一项配置
     *
     * <p>会按配置键的语义校验取值（例如 {@code audit_required} 只能是 0/1、
     * 时长类不能为负），不合法直接抛参数异常，避免把库改成一个跑不通的状态。</p>
     */
    void update(BookingConfigUpdateDTO dto);

    /**
     * 把配置恢复为初始默认值（演示/答辩时一键还原用）
     *
     * @return 被恢复的配置项数
     */
    int resetToDefault();

    /** 清空配置缓存（管理端修改后立即生效的关键一步） */
    void evictCache();
}
