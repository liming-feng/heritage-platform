package com.lmf.heritage.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lmf.heritage.module.review.dto.ReviewQueryDTO;
import com.lmf.heritage.module.review.entity.Review;
import com.lmf.heritage.module.review.vo.CraftsmanReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

/**
 * 评价 Mapper
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 某评价对象的平均分（保留 1 位小数）
     *
     * @return 无评价时返回 null
     */
    @Select("SELECT ROUND(AVG(rating), 1) FROM review "
            + "WHERE type = #{type} AND target_id = #{targetId} AND deleted = 0")
    Double selectAvgRating(@Param("type") Integer type, @Param("targetId") Long targetId);

    /** 某评价对象的评价条数 */
    @Select("SELECT COUNT(*) FROM review "
            + "WHERE type = #{type} AND target_id = #{targetId} AND deleted = 0")
    long countByTarget(@Param("type") Integer type, @Param("targetId") Long targetId);

    /**
     * 「有用」点赞数 +1
     *
     * <p>用 {@code like_count = like_count + 1} 而不是「先查再写」，
     * 避免多人同时点赞时互相覆盖（自增在同一条 UPDATE 内完成）。</p>
     *
     * @return 受影响行数，0 表示评价不存在
     */
    @Update("UPDATE review SET like_count = like_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementLike(@Param("id") Long id);

    /**
     * 批量查「哪些预约已经评价过」
     *
     * <p>预约列表要给每一行算「是否可评价」，如果逐条去查就是典型的 N+1；
     * 这里一次把当页所有预约的评价情况查出来，由业务层放进 Set 判断。</p>
     */
    @Select("<script>"
            + "SELECT booking_id FROM review WHERE deleted = 0 AND type = 2 AND booking_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    java.util.List<Long> selectReviewedBookingIds(@Param("ids") java.util.List<Long> ids);

    /** 某条预约是否已评价过（课程评价以 booking_id 为事实来源） */
    @Select("SELECT COUNT(*) FROM review WHERE deleted = 0 AND type = 2 AND booking_id = #{bookingId}")
    int countByBooking(@Param("bookingId") Long bookingId);

    /* ==================== 匠人端评价管理（P7） ==================== */

    /**
     * 匠人端评价列表分页
     *
     * <p>「某匠人收到的评价」横跨两张业务表：商品评价看 {@code goods.craftsman_id}，
     * 课程评价看 {@code course.craftsman_id}。所以归属判断不能只写一个 join 条件，
     * 而要按 {@code type} 分流——这也是评价表用 type + target_id 统一存储的代价，
     * 代价换来的是评价的字段、筛选、回复逻辑只有一份。</p>
     */
    IPage<CraftsmanReviewVO> selectCraftsmanReviewPage(IPage<CraftsmanReviewVO> page,
                                                       @Param("craftsmanId") Long craftsmanId,
                                                       @Param("q") ReviewQueryDTO query);

    /**
     * 匠人端评价统计
     *
     * <p>返回原始聚合值（Map）而不是直接给 VO：星级分布是「5 星数量、4 星数量……
     * 加上各自占比」，占比必须由总数在 Java 侧算，SQL 里再算一遍容易与 total 打架。
     * 一条 SQL 把需要的计数全部取回来，组装交给服务层。</p>
     */
    Map<String, Object> selectCraftsmanStat(@Param("craftsmanId") Long craftsmanId);

    /**
     * 一条评价所属的匠人 id
     *
     * <p>评价表用 type 区分商品评价与课程评价，归属因此要按 type 分流。
     * 与其在服务层写 {@code if (type == 1) 查商品 else 查课程}，
     * 不如让一条 SQL 把结论给出来——分支只存在于 SQL 里，Java 侧只需要判空。</p>
     *
     * <p>刻意<b>不带</b> {@code g.deleted = 0} 之类的过滤：归属判断是权限问题，
     * 商品被下架不该让「这条评价是谁的」变得无从判断。真查不到（记录已硬删）才返回 null。</p>
     */
    @Select("SELECT CASE WHEN r.type = 1 THEN g.craftsman_id ELSE c.craftsman_id END "
            + "FROM review r "
            + "LEFT JOIN goods  g ON r.type = 1 AND g.id = r.target_id "
            + "LEFT JOIN course c ON r.type = 2 AND c.id = r.target_id "
            + "WHERE r.id = #{reviewId} AND r.deleted = 0")
    Long selectOwnerCraftsmanId(@Param("reviewId") Long reviewId);
}
