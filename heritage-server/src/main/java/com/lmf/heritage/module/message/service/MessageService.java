package com.lmf.heritage.module.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lmf.heritage.common.PageResult;
import com.lmf.heritage.module.message.dto.MessageQueryDTO;
import com.lmf.heritage.module.message.entity.Message;
import com.lmf.heritage.module.message.vo.MessageVO;

/**
 * 站内消息服务
 *
 * <p>预约的每一次状态变更都会经由这里落一条消息。做法上只有一条原则：
 * <b>发消息失败绝不能影响主业务</b>——审核该通过还是要通过，
 * 不能因为消息表写不进去就把预约回滚掉。</p>
 *
 * @author 李明峰 23100025
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送站内消息（内部已做异常兜底，不会向调用方抛出）
     *
     * @param userId    接收人
     * @param type      消息类型，见 {@code Constants.MSG_*}
     * @param title     标题
     * @param content   内容
     * @param relatedId 关联业务 id（预约消息传 booking.id）
     */
    void send(Long userId, int type, String title, String content, Long relatedId);

    /** 我的消息分页 */
    PageResult<MessageVO> page(MessageQueryDTO query);

    /** 未读消息数 */
    long unreadCount();

    /**
     * 按类型统计消息数与未读数
     *
     * <p>消息中心有「预约 / 订单 / 优惠券 / 系统」几个分类 tab，
     * 每个 tab 上要显示总数与未读小红点，这里一次返回全部分类。</p>
     */
    java.util.List<com.lmf.heritage.module.message.vo.MessageTypeStatVO> typeStat();

    /** 单条标记已读（只能标记自己的消息） */
    void markRead(Long messageId);

    /** 全部标记已读 */
    int markAllRead();

    /**
     * 判断某张业务单据是否已就同一类型发过消息
     *
     * <p>定时任务每轮都可能重复扫到同一批数据，靠这个做幂等判断，
     * 避免用户收到一串重复的开课提醒。</p>
     */
    boolean alreadySent(Long userId, int type, Long relatedId);

    /**
     * 带标题的幂等判断：同一张单据在<b>同一消息类型下有多种子通知</b>时用它区分
     *
     * <p>典型场景是优惠券：{@code related_id} 都是用户券 id，
     * 「券到账」与「券即将过期」只能靠标题区分，否则前者会把后者挡掉。</p>
     */
    boolean alreadySent(Long userId, int type, Long relatedId, String title);
}
