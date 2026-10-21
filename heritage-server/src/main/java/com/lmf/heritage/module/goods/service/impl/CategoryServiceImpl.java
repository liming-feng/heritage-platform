package com.lmf.heritage.module.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.course.mapper.CourseMapper;
import com.lmf.heritage.module.goods.dto.CategoryBatchSortDTO;
import com.lmf.heritage.module.goods.dto.CategorySaveDTO;
import com.lmf.heritage.module.goods.entity.Category;
import com.lmf.heritage.module.goods.mapper.CategoryMapper;
import com.lmf.heritage.module.goods.mapper.GoodsMapper;
import com.lmf.heritage.module.goods.service.CategoryService;
import com.lmf.heritage.module.goods.vo.AdminCategoryVO;
import com.lmf.heritage.module.goods.vo.CategoryVO;
import com.lmf.heritage.util.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 非遗分类服务实现
 *
 * @author 李明峰 23100025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final GoodsMapper goodsMapper;
    private final CourseMapper courseMapper;
    private final CacheHelper cache;

    @Override
    public List<CategoryVO> listEnabled() {
        return cache.getListOrLoad(
                Constants.KEY_HOME_CATEGORIES,
                CategoryVO.class,
                Constants.TTL_CATEGORIES,
                this::loadFromDb);
    }

    @Override
    public CategoryVO getVO(Long id) {
        if (id == null) {
            return null;
        }
        Category c = getById(id);
        if (c == null) {
            return null;
        }
        CategoryVO vo = toVO(c);
        vo.setGoodsCount(countMap().getOrDefault(c.getId(), 0));
        return vo;
    }

    @Override
    public void evictCache() {
        cache.delete(Constants.KEY_HOME_CATEGORIES);
    }

    /* ------------------------------------------------------------------ */

    /* ==================== 管理端（P8） ==================== */

    @Override
    public List<AdminCategoryVO> adminList() {
        // 不走缓存：管理端要「改完立即看到」，读到 1 小时前的快照会让人以为没生效
        List<Category> list = list(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        Map<Long, Integer> onSale = countMap();
        Map<Long, Integer> all = countMap(goodsMapper.countAllGroupByCategory());
        Map<Long, Integer> courses = countMap(courseMapper.countGroupByCategory());

        return list.stream().map(c -> {
            AdminCategoryVO vo = new AdminCategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setIcon(c.getIcon());
            vo.setDescription(c.getDescription());
            vo.setSort(c.getSort());
            vo.setStatus(c.getStatus());
            vo.setStatusText(Integer.valueOf(Constants.STATUS_ENABLED).equals(c.getStatus()) ? "启用" : "停用");
            vo.setGoodsCount(onSale.getOrDefault(c.getId(), 0));
            vo.setTotalGoodsCount(all.getOrDefault(c.getId(), 0));
            vo.setCourseCount(courses.getOrDefault(c.getId(), 0));
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCategory(CategorySaveDTO dto) {
        dto.normalize();

        // 名称唯一：category 表没有唯一索引（历史原因），只能在这一层守。
        // 放进来两个「剪纸」分类，前台的分类导航会立刻出现两个一模一样的入口
        Long dup = count(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, dto.getName())
                .ne(dto.getId() != null, Category::getId, dto.getId()));
        BusinessException.throwIf(dup != null && dup > 0, ErrorCode.CATEGORY_NAME_EXISTS,
                "分类「" + dto.getName() + "」已存在");

        Category entity = new Category();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setIcon(dto.getIcon());
        entity.setDescription(dto.getDescription());

        if (dto.getId() == null) {
            // 新建：不传排序号时排到最后，而不是排到最前（默认 0 会插队到首位）
            entity.setSort(dto.getSort() != null ? dto.getSort() : nextSort());
            entity.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLED);
            save(entity);
        } else {
            BusinessException.throwIf(getById(dto.getId()) == null, ErrorCode.CATEGORY_NOT_FOUND);
            // 编辑时字段为空即「保持原值」（updateById 会忽略 null），
            // 因此只在显式传了值时才写入，避免把排序号或状态冲成 null
            entity.setSort(dto.getSort());
            entity.setStatus(dto.getStatus());
            updateById(entity);
        }

        evictCache();
        log.info("[分类管理] {} 分类 #{}「{}」", dto.getId() == null ? "新增" : "编辑",
                entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(getById(id) == null, ErrorCode.CATEGORY_NOT_FOUND);
        Category update = new Category();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        evictCache();
        log.info("[分类管理] 分类 #{} 状态改为 {}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSort(CategoryBatchSortDTO dto) {
        int changed = 0;
        for (CategoryBatchSortDTO.Item item : dto.getItems()) {
            if (item.getId() == null || (item.getSort() == null && item.getStatus() == null)) {
                continue;
            }
            Category update = new Category();
            update.setId(item.getId());
            update.setSort(item.getSort());
            update.setStatus(item.getStatus());
            changed += updateById(update) ? 1 : 0;
        }
        BusinessException.throwIf(changed == 0, ErrorCode.ADMIN_OPERATION_EMPTY, "没有需要更新的分类");
        evictCache();
        log.info("[分类管理] 批量调整 {} 个分类", changed);
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCategory(Long id) {
        Category category = getById(id);
        BusinessException.throwIf(category == null, ErrorCode.CATEGORY_NOT_FOUND);

        // 用「商品总数」而不是「在售数」判断：一件下架商品同样挂在这个分类下，
        // 删掉分类它就成了无主数据
        long goodsTotal = goodsMapper.selectCount(Wrappers.<com.lmf.heritage.module.goods.entity.Goods>lambdaQuery()
                .eq(com.lmf.heritage.module.goods.entity.Goods::getCategoryId, id));
        BusinessException.throwIf(goodsTotal > 0, ErrorCode.CATEGORY_HAS_GOODS,
                "分类「" + category.getName() + "」下还有 " + goodsTotal + " 件商品，无法删除");

        long courseTotal = courseMapper.selectCount(
                Wrappers.<com.lmf.heritage.module.course.entity.Course>lambdaQuery()
                        .eq(com.lmf.heritage.module.course.entity.Course::getCategoryId, id));
        BusinessException.throwIf(courseTotal > 0, ErrorCode.CATEGORY_HAS_COURSE,
                "分类「" + category.getName() + "」下还有 " + courseTotal + " 门课程，无法删除");

        removeById(id);
        evictCache();
        log.warn("[分类管理] 删除分类 #{}「{}」", id, category.getName());
    }

    /** 新增分类时的默认排序号：现有最大 sort + 1，保证排到最后 */
    private int nextSort() {
        return list().stream()
                .map(Category::getSort)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    /* ------------------------------------------------------------------ */

    /** 回源：查启用中的分类 + 各分类在售商品数 */
    private List<CategoryVO> loadFromDb() {
        List<Category> list = list(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        if (list.isEmpty()) {
            log.warn("分类表里没有启用中的分类，检查 category 表初始化数据");
            return List.of();
        }

        Map<Long, Integer> counts = countMap();
        return list.stream().map(c -> {
            CategoryVO vo = toVO(c);
            vo.setGoodsCount(counts.getOrDefault(c.getId(), 0));
            return vo;
        }).toList();
    }

    /** 各分类在售商品数（前台角标口径） */
    private Map<Long, Integer> countMap() {
        return countMap(goodsMapper.countGroupByCategory());
    }

    /**
     * 把 {@code GROUP BY category_id} 的查询结果转成 map
     *
     * <p>刻意不把 SQL 写死在这个方法里：分类管理页要同时用到三种口径
     * （在售商品 / 全部商品 / 课程），抽成一个转换器比复制三段一样的循环干净。</p>
     */
    private Map<Long, Integer> countMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object cid = row.get("categoryId");
            Object cnt = row.get("cnt");
            if (cid instanceof Number n1 && cnt instanceof Number n2) {
                map.put(n1.longValue(), n2.intValue());
            }
        }
        return map;
    }

    private CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setDescription(c.getDescription());
        return vo;
    }
}
