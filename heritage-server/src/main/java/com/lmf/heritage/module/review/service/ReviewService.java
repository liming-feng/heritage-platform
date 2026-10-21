package com.lmf.heritage.module.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.review.dto.ReviewPublishCourseDTO;
import com.lmf.heritage.module.review.dto.ReviewPublishDTO;
import com.lmf.heritage.module.review.dto.ReviewQueryDTO;
import com.lmf.heritage.module.review.dto.ReviewReplyDTO;
import com.lmf.heritage.module.review.entity.Review;
import com.lmf.heritage.module.review.vo.CraftsmanReviewVO;
import com.lmf.heritage.module.review.vo.ReviewStatVO;
import com.lmf.heritage.module.review.vo.ReviewVO;

import java.util.List;

/**
 * 评价服务（P3 只做读取，写入在 P4 随订单一起做）
 *
 * @author 李明峰 23100025
 */
public interface ReviewService extends IService<Review> {

    /** 按评价对象分页查询 */
    PageResult<ReviewVO> pageByTarget(Integer type, Long targetId, long pageNum, long pageSize);

    /** 取最新 N 条评价（商品详情页用） */
    List<ReviewVO> listByTarget(Integer type, Long targetId, int limit);

    /** 平均分，无评价返回 null */
    Double avgRating(Integer type, Long targetId);

    /** 评价条数 */
    long countByTarget(Integer type, Long targetId);

    /**
     * 发表商品评价
     *
     * <p>必须来自本人**已完成**订单中的订单项，且该订单项未被评价过，
     * 评价成功后会标记 {@code order_item.reviewed = 1}。</p>
     */
    void publish(ReviewPublishDTO dto);

    /**
     * 发表课程评价（P5 的收尾环节：课程结束 → 预约已完成 → 用户评价）
     *
     * <p>与商品评价同一套思路：不信任前端传来的课程 id，而是回到「预约记录」这个事实来源上校验
     * ——预约属于本人、状态为已完成、且该预约尚未评价过。</p>
     */
    void publishCourse(ReviewPublishCourseDTO dto);

    /**
     * 「有用」点赞
     *
     * <p>用 Redis Set 记录点过赞的用户，同一人重复点击会被拒绝（防刷赞）。</p>
     */
    void like(Long reviewId);

    /* ==================== 匠人端评价管理（P7） ==================== */

    /**
     * 匠人端评价列表
     *
     * <p>返回「本匠人的商品与课程收到的评价」，支持按类型、评价对象、星级、口碑档位、
     * 是否带图、是否已回复、关键词筛选。</p>
     */
    PageResult<CraftsmanReviewVO> craftsmanReviews(ReviewQueryDTO query);

    /**
     * 回复 / 修改回复
     *
     * <p>同一个方法覆盖两种情况：{@code review.reply} 为空是首次回复，非空是修改。
     * 回复前会校验这条评价确实落在本匠人的商品或课程上。</p>
     */
    void reply(ReviewReplyDTO dto);

    /** 本匠人的评价统计：平均分、评价数、好评率、星级分布、回复情况 */
    ReviewStatVO craftsmanStat();
}
