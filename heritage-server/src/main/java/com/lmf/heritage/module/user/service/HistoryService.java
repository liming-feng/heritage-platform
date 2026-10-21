package com.lmf.heritage.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.module.user.dto.HistoryUploadDTO;
import com.lmf.heritage.module.user.entity.BrowseHistory;
import com.lmf.heritage.module.user.vo.HistoryGroupVO;

import java.util.List;

/**
 * 浏览历史服务
 *
 * <p>未登录时前端把记录存在 localStorage（最多 50 条），登录后调 upload 一次性上报。</p>
 *
 * @author 李明峰 23100025
 */
public interface HistoryService extends IService<BrowseHistory> {

    /**
     * 批量上报浏览记录
     *
     * <p>按 {@code (user_id, target_type, target_id)} 去重：已存在则只刷新浏览时间，不新增行。</p>
     *
     * @return 实际处理的条数
     */
    int upload(HistoryUploadDTO dto);

    /**
     * 浏览历史列表，按日期分组返回（今天 / 昨天 / 具体日期）
     *
     * @param targetType 可选，按类型过滤（1商品 2课程 3资讯）
     */
    List<HistoryGroupVO> listGrouped(Integer targetType);

    /** 删除一条历史 */
    void delete(Integer targetType, Long targetId);

    /** 清空当前用户全部历史 */
    void clear();
}
