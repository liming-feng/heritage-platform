package com.lmf.heritage.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.content.dto.AdminBannerQueryDTO;
import com.lmf.heritage.module.content.dto.BannerBatchSortDTO;
import com.lmf.heritage.module.content.dto.BannerSaveDTO;
import com.lmf.heritage.module.content.entity.Banner;
import com.lmf.heritage.module.content.mapper.BannerMapper;
import com.lmf.heritage.module.content.service.AdminBannerService;
import com.lmf.heritage.module.content.service.BannerService;
import com.lmf.heritage.module.content.vo.AdminBannerVO;
import com.lmf.heritage.module.content.vo.BannerExcelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端 · 轮播图管理实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerServiceImpl implements AdminBannerService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出上限 */
    private static final int EXPORT_LIMIT = 1000;

    private final BannerMapper bannerMapper;
    private final BannerService bannerService;

    /* ==================== 列表与详情 ==================== */

    @Override
    public PageResult<AdminBannerVO> page(AdminBannerQueryDTO query) {
        query.normalize();
        IPage<AdminBannerVO> result = queryPage(query, query.getPageNum(), query.getPageSize());
        return PageResult.of(result, result.getRecords());
    }

    /** 查询 + 装饰（分页与导出共用，理由同资讯管理） */
    private IPage<AdminBannerVO> queryPage(AdminBannerQueryDTO query, Long pageNum, Long pageSize) {
        Page<AdminBannerVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminBannerVO> result = bannerMapper.selectAdminBannerPage(page, query);
        result.getRecords().forEach(this::decorate);
        return result;
    }

    @Override
    public AdminBannerVO detail(Long id) {
        Banner banner = bannerMapper.selectById(id);
        BusinessException.throwIf(banner == null, ErrorCode.BANNER_NOT_FOUND);
        AdminBannerVO vo = toVo(banner);
        decorate(vo);
        return vo;
    }

    /* ==================== 新增 / 编辑 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(BannerSaveDTO dto) {
        dto.normalize();
        if (dto.getPosition() != null) {
            BusinessException.throwIf(dto.getPosition() != 1 && dto.getPosition() != 2,
                    ErrorCode.PARAM_ERROR, "position 只能是 1（首页）或 2（课程页）");
        }

        boolean isNew = dto.getId() == null;
        Banner banner;
        if (isNew) {
            banner = new Banner();
            banner.setPosition(dto.getPosition() != null ? dto.getPosition() : 1);
            banner.setSort(dto.getSort() != null ? dto.getSort() : bannerMapper.nextSort(banner.getPosition()));
            banner.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLED);
        } else {
            banner = bannerMapper.selectById(dto.getId());
            BusinessException.throwIf(banner == null, ErrorCode.BANNER_NOT_FOUND);
            // 改位置时排序号要重新落到新位置的末尾：原排序号在新位置上可能撞车，
            // 也可能直接把新位置第一屏顶掉
            if (dto.getPosition() != null && !dto.getPosition().equals(banner.getPosition())) {
                banner.setPosition(dto.getPosition());
                banner.setSort(dto.getSort() != null ? dto.getSort() : bannerMapper.nextSort(dto.getPosition()));
            } else if (dto.getSort() != null) {
                banner.setSort(dto.getSort());
            }
            if (dto.getStatus() != null) {
                banner.setStatus(dto.getStatus());
            }
        }

        banner.setTitle(dto.getTitle());
        banner.setImage(dto.getImage());
        banner.setLinkUrl(dto.getLinkUrl());

        if (isNew) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }

        bannerService.evictCache();
        log.info("[轮播管理] {}轮播 #{}（{}）position={} sort={} status={}",
                isNew ? "新增" : "编辑", banner.getId(), banner.getTitle(),
                banner.getPosition(), banner.getSort(), banner.getStatus());
        return banner.getId();
    }

    /* ==================== 启停 / 排序 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireValidStatus(status);
        Banner banner = requireBanner(id);
        banner.setStatus(status);
        bannerMapper.updateById(banner);
        bannerService.evictCache();
        log.info("[轮播管理] 轮播 #{} {}（position={}）",
                id, status == Constants.STATUS_ENABLED ? "启用" : "停用", banner.getPosition());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSort(BannerBatchSortDTO dto) {
        BusinessException.throwIf(dto.getItems() == null || dto.getItems().isEmpty(),
                ErrorCode.ADMIN_OPERATION_EMPTY);

        int affected = 0;
        for (BannerBatchSortDTO.Item item : dto.getItems()) {
            // 两项都不传说明这一项没有要改的东西，跳过而不是把 sort/status 写成 null
            if (item.getSort() == null && item.getStatus() == null) {
                continue;
            }
            if (item.getStatus() != null) {
                requireValidStatus(item.getStatus());
            }
            LambdaUpdateWrapper<Banner> uw = new LambdaUpdateWrapper<Banner>()
                    .eq(Banner::getId, item.getId());
            if (item.getSort() != null) {
                uw.set(Banner::getSort, item.getSort());
            }
            if (item.getStatus() != null) {
                uw.set(Banner::getStatus, item.getStatus());
            }
            affected += bannerMapper.update(null, uw);
        }
        bannerService.evictCache();
        log.info("[轮播管理] 批量调整 {} 项，生效 {} 条", dto.getItems().size(), affected);
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Banner banner = requireBanner(id);
        bannerMapper.deleteById(id);
        bannerService.evictCache();
        log.warn("[轮播管理] 删除轮播 #{}（{}）", id, banner.getTitle());
    }

    /* ==================== 导出 ==================== */

    @Override
    public List<BannerExcelVO> exportList(AdminBannerQueryDTO query) {
        query.normalize();
        List<AdminBannerVO> records = queryPage(query, 1L, (long) EXPORT_LIMIT).getRecords();
        return records.stream().map(v -> {
            BannerExcelVO e = new BannerExcelVO();
            e.setId(v.getId());
            e.setTitle(v.getTitle());
            e.setImage(v.getImage());
            e.setLinkUrl(v.getLinkUrl());
            e.setPositionDesc(v.getPositionDesc());
            e.setSort(v.getSort());
            e.setStatusDesc(v.getStatusDesc());
            e.setCreateTimeText(v.getCreateTime() == null ? null : v.getCreateTime().format(DT));
            return e;
        }).collect(Collectors.toList());
    }

    /* ==================== 内部工具 ==================== */

    private static AdminBannerVO toVo(Banner b) {
        AdminBannerVO vo = new AdminBannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImage(b.getImage());
        vo.setLinkUrl(b.getLinkUrl());
        vo.setPosition(b.getPosition());
        vo.setSort(b.getSort());
        vo.setStatus(b.getStatus());
        vo.setCreateTime(b.getCreateTime());
        vo.setUpdateTime(b.getUpdateTime());
        return vo;
    }

    private void decorate(AdminBannerVO vo) {
        vo.setPositionDesc(Integer.valueOf(2).equals(vo.getPosition()) ? "课程页" : "首页");
        vo.setStatusDesc(Integer.valueOf(Constants.STATUS_ENABLED).equals(vo.getStatus()) ? "启用" : "停用");
    }

    private Banner requireBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        BusinessException.throwIf(banner == null, ErrorCode.BANNER_NOT_FOUND);
        return banner;
    }

    private static void requireValidStatus(Integer status) {
        BusinessException.throwIf(status == null
                        || (status != Constants.STATUS_DISABLED && status != Constants.STATUS_ENABLED),
                ErrorCode.PARAM_ERROR, "status 只能是 0（停用）或 1（启用）");
    }
}
