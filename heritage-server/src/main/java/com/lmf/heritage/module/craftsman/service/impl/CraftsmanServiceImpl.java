package com.lmf.heritage.module.craftsman.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.course.service.CourseService;
import com.lmf.heritage.module.craftsman.dto.CraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.entity.CraftsmanFollow;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanFollowMapper;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.module.craftsman.service.CraftsmanService;
import com.lmf.heritage.module.craftsman.vo.CraftsmanDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import com.lmf.heritage.module.goods.dto.GoodsQueryDTO;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.service.GoodsService;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 匠人服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CraftsmanServiceImpl extends ServiceImpl<CraftsmanMapper, Craftsman> implements CraftsmanService {

    private final CraftsmanMapper craftsmanMapper;
    private final CraftsmanFollowMapper followMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsService goodsService;
    private final CourseService courseService;

    @Override
    public PageResult<CraftsmanSimpleVO> pageList(CraftsmanQueryDTO query) {
        query.normalize();
        IPage<CraftsmanSimpleVO> page = craftsmanMapper.selectCraftsmanPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        // 用两参重载：自定义分页查询返回的是 IPage，单参版本只接受 Page
        return PageResult.of(page, page.getRecords());
    }

    @Override
    public CraftsmanDetailVO detail(Long id) {
        Craftsman craftsman = craftsmanMapper.selectById(id);
        // 未通过资质审核的匠人不对外展示
        BusinessException.throwIf(craftsman == null || !Integer.valueOf(1).equals(craftsman.getAuditStatus()),
                ErrorCode.CRAFTSMAN_NOT_AUDITED);

        CraftsmanDetailVO vo = new CraftsmanDetailVO();
        vo.setId(craftsman.getId());
        vo.setRealName(craftsman.getRealName());
        vo.setCraftType(craftsman.getCraftType());
        vo.setInheritance(craftsman.getInheritance());
        vo.setIntro(craftsman.getIntro());
        vo.setResume(craftsman.getResume());
        vo.setCover(craftsman.getCover());
        vo.setFollowerCount(craftsman.getFollowerCount());
        vo.setJoinTime(craftsman.getCreateTime());
        vo.setGoodsCount(goodsMapper.countByCraftsman(id));
        vo.setCourseCount(courseService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.lmf.heritage.module.course.entity.Course>()
                .eq(com.lmf.heritage.module.course.entity.Course::getCraftsmanId, id)
                .eq(com.lmf.heritage.module.course.entity.Course::getAuditStatus, 1)));

        // 代表作 / 在售商品：直接复用商品列表，排序与筛选规则保持一致
        GoodsQueryDTO goodsQuery = new GoodsQueryDTO();
        goodsQuery.setCraftsmanId(id);
        goodsQuery.setPageNum(1L);
        goodsQuery.setPageSize(8L);
        vo.setGoodsList(goodsService.pageList(goodsQuery).getRecords());

        vo.setCourseList(courseService.listByCraftsman(id, 6));

        // 未登录时恒为 false，不报错——匠人主页是允许游客浏览的
        Long userId = UserContext.userIdOrNull();
        vo.setFollowed(userId != null && followMapper.countFollowing(userId, id) > 0);
        return vo;
    }

    /**
     * 关注匠人
     *
     * <p>表上有唯一键 {@code uk_user_craftsman}，而取消关注走逻辑删除，
     * 所以「关注 → 取关 → 再关注」时直接 INSERT 会撞唯一键。
     * 处理方式与购物车一致：把已取消的那条<b>复活</b>，而不是插新记录。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long craftsmanId) {
        Long userId = UserContext.userId();
        requireCraftsman(craftsmanId);

        CraftsmanFollow exist = followMapper.selectIncludingDeleted(userId, craftsmanId);
        if (exist != null) {
            if (followMapper.countFollowing(userId, craftsmanId) > 0) {
                // 已经关注中，直接返回（幂等，前端重复点击不会报错）
                return;
            }
            followMapper.revive(exist.getId());
        } else {
            CraftsmanFollow follow = new CraftsmanFollow();
            follow.setUserId(userId);
            follow.setCraftsmanId(craftsmanId);
            followMapper.insert(follow);
        }
        // 关注数用单条自增 SQL，避免并发关注时互相覆盖
        craftsmanMapper.incrFollowerCount(craftsmanId, 1);
        log.info("[匠人] 用户 {} 关注匠人 {}", userId, craftsmanId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long craftsmanId) {
        Long userId = UserContext.userId();
        CraftsmanFollow exist = followMapper.selectIncludingDeleted(userId, craftsmanId);
        if (exist == null || followMapper.countFollowing(userId, craftsmanId) == 0) {
            // 本来就没关注，直接返回（幂等）
            return;
        }
        // 逻辑删除走 removeById，逻辑删除字段由 MyBatis-Plus 处理
        followMapper.deleteById(exist.getId());
        craftsmanMapper.incrFollowerCount(craftsmanId, -1);
        log.info("[匠人] 用户 {} 取消关注匠人 {}", userId, craftsmanId);
    }

    @Override
    public List<CraftsmanSimpleVO> myFollows() {
        return followMapper.listFollowed(UserContext.userId());
    }

    private void requireCraftsman(Long craftsmanId) {
        Craftsman craftsman = craftsmanMapper.selectById(craftsmanId);
        BusinessException.throwIf(craftsman == null || !Integer.valueOf(1).equals(craftsman.getAuditStatus()),
                ErrorCode.CRAFTSMAN_NOT_AUDITED);
    }
}
