package com.lmf.heritage.module.craftsman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.craftsman.dto.CraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.vo.CraftsmanDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;

import java.util.List;

/**
 * 匠人服务
 *
 * @author 李明峰 23100025
 */
public interface CraftsmanService extends IService<Craftsman> {

    /** 匠人列表（关键词 / 技艺门类 / 排序 / 分页） */
    PageResult<CraftsmanSimpleVO> pageList(CraftsmanQueryDTO query);

    /**
     * 匠人主页：履历、师承、代表作与在售商品、开设课程、关注状态
     *
     * @param id 匠人 id
     */
    CraftsmanDetailVO detail(Long id);

    /** 关注匠人（重复关注幂等） */
    void follow(Long craftsmanId);

    /** 取消关注 */
    void unfollow(Long craftsmanId);

    /** 我的关注列表 */
    List<CraftsmanSimpleVO> myFollows();
}
