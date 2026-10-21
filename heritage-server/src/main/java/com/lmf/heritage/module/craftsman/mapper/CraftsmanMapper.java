package com.lmf.heritage.module.craftsman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.craftsman.dto.AdminCraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.dto.CraftsmanQueryDTO;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.vo.AdminCraftsmanVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanAuditDetailVO;
import com.lmf.heritage.module.craftsman.vo.CraftsmanSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 匠人 Mapper
 *
 * <p>列表要顺带算出「在售作品数」，用预聚合子查询在 XML 里一次查完
 * （见 {@code resources/mapper/craftsman/CraftsmanMapper.xml}）。</p>
 *
 * @author 李明峰 23100025
 */
@Mapper
public interface CraftsmanMapper extends BaseMapper<Craftsman> {

    /** 匠人列表分页（联表预聚合出在售作品数） */
    IPage<CraftsmanSimpleVO> selectCraftsmanPage(IPage<CraftsmanSimpleVO> page,
                                                 @Param("q") CraftsmanQueryDTO query);

    /**
     * 关注数增减
     *
     * <p>关注 / 取关是高频动作，用一条 SQL 自增而不是「查出来 +1 再写回」，
     * 避免两个人同时点关注时互相覆盖。{@code GREATEST(..., 0)} 保证不会出现负数。</p>
     *
     * @param delta 1 关注 / -1 取关
     */
    @Update("UPDATE craftsman SET follower_count = GREATEST(follower_count + #{delta}, 0), "
            + "update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int incrFollowerCount(@Param("id") Long id, @Param("delta") int delta);

    /* ==================== 管理端 ==================== */

    /** 管理端匠人列表分页：与 user 联表，带出账号状态与登录名 */
    IPage<AdminCraftsmanVO> selectAdminCraftsmanPage(IPage<AdminCraftsmanVO> page,
                                                     @Param("q") AdminCraftsmanQueryDTO query);

    /**
     * 资质审核详情
     *
     * <p>单独一条 SQL 而不是复用列表：只有这里需要 {@code id_card_no} 与 {@code cert_image}，
     * 把敏感字段挂在列表查询上，早晚会有人顺手把它带进导出。</p>
     */
    CraftsmanAuditDetailVO selectAuditDetail(@Param("id") Long id);

    /** 门类下拉选项：从现有数据里取，避免前端硬编码一份和库对不上的字典 */
    @Select("SELECT DISTINCT craft_type FROM craftsman "
            + "WHERE deleted = 0 AND craft_type IS NOT NULL AND craft_type != '' "
            + "ORDER BY craft_type")
    List<String> selectCraftTypes();
}
