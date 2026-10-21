package com.lmf.heritage.module.review.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.review.dto.ReviewPublishCourseDTO;
import com.lmf.heritage.module.review.dto.ReviewPublishDTO;
import com.lmf.heritage.module.review.dto.ReviewQueryDTO;
import com.lmf.heritage.module.review.dto.ReviewReplyDTO;
import com.lmf.heritage.module.review.entity.Review;
import com.lmf.heritage.module.review.mapper.ReviewMapper;
import com.lmf.heritage.module.review.service.ReviewService;
import com.lmf.heritage.module.review.vo.CraftsmanReviewVO;
import com.lmf.heritage.module.review.vo.ReviewStatVO;
import com.lmf.heritage.module.review.vo.ReviewVO;
import com.lmf.heritage.module.trade.entity.Order;
import com.lmf.heritage.module.trade.entity.OrderItem;
import com.lmf.heritage.module.trade.mapper.OrderItemMapper;
import com.lmf.heritage.module.trade.mapper.OrderMapper;
import com.lmf.heritage.module.booking.entity.Booking;
import com.lmf.heritage.module.booking.mapper.BookingMapper;
import com.lmf.heritage.module.user.entity.User;
import com.lmf.heritage.module.user.mapper.UserMapper;
import com.lmf.heritage.security.UserContext;
import com.lmf.heritage.util.CraftsmanHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务实现
 *
 * <p>读取（P3）：评价表里只存 userId，展示要昵称和头像 —— 这里对当页的 userId 做一次批量查询
 * 再内存映射，避免逐条回查用户表（N+1）。</p>
 *
 * <p>写入（P4）：发表评价与点赞。评价资格回到「订单项」这个事实来源上校验，
 * 而不是信任前端传来的商品 id。</p>
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    /** 解析 review.images 里存的 JSON 数组 */
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 点赞去重集合的保留时长 */
    private static final Duration LIKE_KEY_TTL = Duration.ofDays(30);

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BookingMapper bookingMapper;
    private final StringRedisTemplate redisTemplate;
    private final CraftsmanHelper craftsmanHelper;

    @Override
    public PageResult<ReviewVO> pageByTarget(Integer type, Long targetId, long pageNum, long pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        IPage<Review> result = page(page, Wrappers.<Review>lambdaQuery()
                .eq(Review::getType, type)
                .eq(Review::getTargetId, targetId)
                .orderByDesc(Review::getCreateTime)
                .orderByDesc(Review::getId));

        Map<Long, User> userMap = loadUsers(result.getRecords());
        List<ReviewVO> list = result.getRecords().stream()
                .map(r -> toVO(r, userMap.get(r.getUserId())))
                .toList();
        return PageResult.of(result, list);
    }

    @Override
    public List<ReviewVO> listByTarget(Integer type, Long targetId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Page<Review> page = new Page<>(1, limit, false);
        IPage<Review> result = page(page, Wrappers.<Review>lambdaQuery()
                .eq(Review::getType, type)
                .eq(Review::getTargetId, targetId)
                .orderByDesc(Review::getCreateTime)
                .orderByDesc(Review::getId));
        if (result.getRecords().isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = loadUsers(result.getRecords());
        return result.getRecords().stream()
                .map(r -> toVO(r, userMap.get(r.getUserId())))
                .toList();
    }

    @Override
    public Double avgRating(Integer type, Long targetId) {
        return baseMapper.selectAvgRating(type, targetId);
    }

    @Override
    public long countByTarget(Integer type, Long targetId) {
        return baseMapper.countByTarget(type, targetId);
    }

    /* ==================== P4 新增：发表评价与点赞 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(ReviewPublishDTO dto) {
        Long userId = UserContext.userId();

        // 评价资格不信任前端传来的商品 id，回到「订单项」这个事实来源上校验
        OrderItem item = orderItemMapper.selectById(dto.getOrderItemId());
        BusinessException.throwIf(item == null, ErrorCode.REVIEW_NOT_FOUND, "订单项不存在");

        Order order = orderMapper.selectById(item.getOrderId());
        BusinessException.throwIf(order == null, ErrorCode.ORDER_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(order.getUserId()),
                ErrorCode.ORDER_NOT_BELONG_TO_YOU, "只能评价自己订单里的商品");
        BusinessException.throwIf(!Integer.valueOf(Constants.ORDER_FINISHED).equals(order.getStatus()),
                ErrorCode.ORDER_STATUS_ERROR, "订单完成后才能评价");
        BusinessException.throwIf(Integer.valueOf(1).equals(item.getReviewed()),
                ErrorCode.REVIEW_ALREADY_EXISTS);

        Review review = new Review();
        review.setType(Constants.REVIEW_TYPE_GOODS);
        review.setTargetId(item.getGoodsId());
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(writeImages(dto.getImages()));
        review.setLikeCount(0);
        save(review);

        // 标记订单项已评价（条件更新 reviewed = 0 -> 1）
        orderItemMapper.markReviewed(item.getId());

        // 商品详情接口是按需实时聚合评价的（评价列表没有整体缓存），所以这里无需清缓存；
        // 若后续给商品详情加缓存，记得在这里一并清理。
        log.info("[评价] 用户 {} 评价订单项 {} 商品 {} 评分 {}",
                userId, item.getId(), item.getGoodsId(), dto.getRating());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishCourse(ReviewPublishCourseDTO dto) {
        Long userId = UserContext.userId();

        // 评价资格回到「预约记录」这个事实来源：属于本人 + 已完成 + 未评价过
        Booking booking = bookingMapper.selectById(dto.getBookingId());
        BusinessException.throwIf(booking == null, ErrorCode.BOOKING_NOT_FOUND);
        BusinessException.throwIf(!userId.equals(booking.getUserId()),
                ErrorCode.BOOKING_NOT_BELONG_TO_YOU, "只能评价自己的预约");
        BusinessException.throwIf(!Integer.valueOf(Constants.BOOKING_FINISHED).equals(booking.getStatus()),
                ErrorCode.BOOKING_STATUS_ERROR, "课程结束后才能评价，当前为「"
                        + com.lmf.heritage.module.booking.enums.BookingStatusEnum.of(booking.getStatus()).getDesc() + "」");
        BusinessException.throwIf(baseMapper.countByBooking(booking.getId()) > 0,
                ErrorCode.REVIEW_ALREADY_EXISTS);

        Review review = new Review();
        review.setType(Constants.REVIEW_TYPE_COURSE);
        review.setTargetId(booking.getCourseId());
        review.setBookingId(booking.getId());
        review.setUserId(userId);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(writeImages(dto.getImages()));
        review.setLikeCount(0);
        save(review);

        log.info("[评价] 用户 {} 评价预约 {}（课程 {}）评分 {}",
                userId, booking.getBookingNo(), booking.getCourseId(), dto.getRating());
    }

    @Override
    public void like(Long reviewId) {
        Long userId = UserContext.userId();
        BusinessException.throwIf(baseMapper.selectById(reviewId) == null, ErrorCode.REVIEW_NOT_FOUND);

        // Redis Set 天然去重：add 返回 1 表示首次点赞，返回 0 表示这个人已经赞过
        String key = Constants.KEY_REVIEW_LIKE + reviewId;
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(userId));
        if (added == null || added == 0) {
            throw new BusinessException(ErrorCode.REPEAT_SUBMIT, "你已经点过赞了");
        }
        redisTemplate.expire(key, LIKE_KEY_TTL);

        baseMapper.incrementLike(reviewId);
        log.debug("[评价] 用户 {} 点赞评价 {}", userId, reviewId);
    }

    /* ==================== 匠人端评价管理（P7） ==================== */

    @Override
    public PageResult<CraftsmanReviewVO> craftsmanReviews(ReviewQueryDTO query) {
        query.normalize();
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();

        Page<CraftsmanReviewVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<CraftsmanReviewVO> result = baseMapper.selectCraftsmanReviewPage(page, craftsmanId, query);

        List<CraftsmanReviewVO> records = result.getRecords();
        records.forEach(this::decorate);
        return PageResult.of(result, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(ReviewReplyDTO dto) {
        Review review = baseMapper.selectById(dto.getReviewId());
        BusinessException.throwIf(review == null, ErrorCode.REVIEW_NOT_FOUND);

        // 归属校验：这条评价必须落在我自己的商品或课程上。
        // 用一条 SQL 把「评价 → 归属匠人」查出来，而不是在这里 new 一个
        // 「type 是 1 就查 goods、是 2 就查 course」的分支——分支越少越不容易漏。
        Long ownerCraftsmanId = baseMapper.selectOwnerCraftsmanId(dto.getReviewId());
        BusinessException.throwIf(ownerCraftsmanId == null,
                ErrorCode.REVIEW_NOT_FOUND, "评价对应的商品或课程已不存在");
        craftsmanHelper.requireSelf(ownerCraftsmanId);

        boolean isUpdate = review.getReply() != null && !review.getReply().isBlank();

        Review update = new Review();
        update.setId(dto.getReviewId());
        update.setReply(dto.getReply().trim());
        // 每次提交都刷新回复时间：前端显示「最后回复于 xx」，改过的回复不该还挂着旧时间
        update.setReplyTime(LocalDateTime.now());
        baseMapper.updateById(update);

        log.info("[评价] 匠人 {} {}评价 {} 的回复", ownerCraftsmanId,
                isUpdate ? "修改" : "回复", dto.getReviewId());
    }

    @Override
    public ReviewStatVO craftsmanStat() {
        Long craftsmanId = craftsmanHelper.currentCraftsmanId();
        Map<String, Object> row = baseMapper.selectCraftsmanStat(craftsmanId);

        ReviewStatVO vo = new ReviewStatVO();
        long total = num(row, "total");
        vo.setTotal(total);
        vo.setAvgRating(dec(row, "avgRating"));

        long good = num(row, "goodCount");
        long mid = num(row, "midCount");
        long bad = num(row, "badCount");
        vo.setGoodCount(good);
        vo.setMidCount(mid);
        vo.setBadCount(bad);
        vo.setGoodRate(percent(good, total));

        vo.setImageCount(num(row, "imageCount"));
        long replied = num(row, "repliedCount");
        vo.setRepliedCount(replied);
        vo.setUnrepliedCount(total - replied);
        vo.setRepliedRate(percent(replied, total));

        // 星级分布从 5 星往 1 星排：前端画柱状图时左高右低，符合阅读习惯
        List<ReviewStatVO.StarDist> dist = new ArrayList<>(5);
        for (int star = 5; star >= 1; star--) {
            long count = num(row, "star" + star);
            dist.add(new ReviewStatVO.StarDist(star, count, percent(count, total)));
        }
        vo.setStarDist(dist);

        log.debug("[评价] 匠人 {} 评价统计：共 {} 条，平均 {} 分，好评率 {}%",
                craftsmanId, total, vo.getAvgRating(), vo.getGoodRate());
        return vo;
    }

    /* ------------------------------------------------------------------ */

    /** 补齐列表项的派生字段：类型文案、配图数组、口碑档位、是否带图/已回复 */
    private void decorate(CraftsmanReviewVO vo) {
        vo.setTypeDesc(Integer.valueOf(Constants.REVIEW_TYPE_COURSE).equals(vo.getType())
                ? "课程评价" : "商品评价");
        vo.setRatingLevel(ratingLevel(vo.getRating()));

        vo.setImages(parseImages(vo.getImagesJson()));
        vo.setHasImage(!vo.getImages().isEmpty());

        boolean replied = vo.getReply() != null && !vo.getReply().isBlank();
        vo.setReplied(replied);
    }

    /** 口碑档位：4-5 星好评 / 3 星中评 / 1-2 星差评 */
    private static String ratingLevel(Integer rating) {
        if (rating == null) {
            return null;
        }
        if (rating >= 4) {
            return "good";
        }
        return rating == 3 ? "mid" : "bad";
    }

    /** 从聚合结果里取整数（SUM 已用 IFNULL 兜底，这里再防一手类型转换异常） */
    private static long num(Map<String, Object> row, String key) {
        Object v = row == null ? null : row.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /** 从聚合结果里取小数 */
    private static double dec(Map<String, Object> row, String key) {
        Object v = row == null ? null : row.get(key);
        return v instanceof Number n ? n.doubleValue() : 0D;
    }

    /**
     * 占比，保留 1 位小数
     *
     * <p>总数 0 时约定返回 0 而不是抛除零异常——「还没有评价」是正常状态，
     * 前端拿到 0 显示成「暂无数据」即可。</p>
     */
    private static double percent(long part, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    /* ------------------------------------------------------------------ */

    /** 批量取评价人信息 */
    private Map<Long, User> loadUsers(List<Review> reviews) {
        List<Long> ids = reviews.stream()
                .map(Review::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private ReviewVO toVO(Review r, User user) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setTargetId(r.getTargetId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setImages(parseImages(r.getImages()));
        vo.setReply(r.getReply());
        vo.setReplyTime(r.getReplyTime());
        vo.setLikeCount(r.getLikeCount());
        vo.setUserId(r.getUserId());
        vo.setCreateTime(r.getCreateTime());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }

    /** 把图片列表写成 JSON 数组存进 review.images（最多 3 张，DTO 已校验） */
    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(images);
        } catch (Exception e) {
            log.warn("评价配图序列化失败，已丢弃。err={}", e.getMessage());
            return null;
        }
    }

    /** 解析图片 JSON 数组，脏数据不抛异常、降级为空列表 */
    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(images, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("评价配图 JSON 解析失败，已忽略。images={}, err={}", images, e.getMessage());
            return List.of();
        }
    }
}
