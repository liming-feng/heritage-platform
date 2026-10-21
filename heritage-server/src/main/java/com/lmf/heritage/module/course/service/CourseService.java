package com.lmf.heritage.module.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.course.dto.CourseQueryDTO;
import com.lmf.heritage.module.course.dto.CourseSaveDTO;
import com.lmf.heritage.module.course.entity.Course;
import com.lmf.heritage.module.course.vo.CourseCalendarVO;
import com.lmf.heritage.module.course.vo.CourseDetailVO;
import com.lmf.heritage.module.course.vo.CourseListVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 课程服务（预约业务的载体）
 *
 * @author 李明峰 23100025
 */
public interface CourseService extends IService<Course> {

    /** 前台课程列表（分类 / 匠人 / 关键词 / 状态 / 只看可预约，带剩余名额与截止倒计时） */
    PageResult<CourseListVO> pageList(CourseQueryDTO query);

    /**
     * 课程详情
     *
     * <p>返回剩余名额、预约截止倒计时，同时把 Redis 浏览量计数器 +1 并叠加到返回值上。</p>
     */
    CourseDetailVO detail(Long id);

    /** 某匠人开设的课程（匠人主页用） */
    List<CourseListVO> listByCraftsman(Long craftsmanId, int limit);

    /** 匠人端：我开设的课程（含未开始的草稿，不受 audit_status 限制） */
    PageResult<CourseListVO> myCourses(CourseQueryDTO query);

    /**
     * 新增 / 编辑课程（匠人端）
     *
     * @return 课程 id
     */
    Long saveCourse(CourseSaveDTO dto);

    /** 删除课程（有未结束的预约时不允许删除） */
    void removeCourse(Long id);

    /* ==================== 匠人端课程运营（P7） ==================== */

    /**
     * 日历排期视图
     *
     * <p>按天分组返回区间内开课的课程，并算好每天的满座率，
     * 便于匠人在日历上一眼看出哪几天排得满、哪几天还空着。</p>
     *
     * @param start 区间起（含）
     * @param end   区间止（含），与 start 相隔不超过 366 天
     */
    List<CourseCalendarVO> calendar(LocalDate start, LocalDate end);

    /**
     * 开放 / 关闭预约
     *
     * <p>落点是 {@code booking_deadline}：关闭预约把截止时间设为此刻，
     * 开放预约把它顶到开课时间——「能不能约」的判定条件没有被复制一份，
     * 仍然只有那一套。</p>
     *
     * @param open true 开放预约 / false 关闭预约
     */
    void setBookingOpen(Long courseId, boolean open);

    /**
     * 提前结束课程
     *
     * <p>用于场地变更、报名不足等需要取消后续安排的场景。正常结束由定时任务按
     * {@code end_time} 自动推进，这里只是让匠人能提前下这个决定。</p>
     *
     * <p>已核销的预约会在下一次定时任务扫描时推进为「已完成」；仍处于待审核 /
     * 已通过的预约不会被静默删除——它们会由超时未审任务拒绝，或由匠人手工处理，
     * 这样用户手里始终有一条可追溯的记录。</p>
     */
    void finishCourse(Long courseId);
}
