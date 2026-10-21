package com.lmf.heritage.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lmf.heritage.common.BusinessException;
import com.lmf.heritage.common.Constants;
import com.lmf.heritage.common.ErrorCode;
import com.lmf.heritage.module.craftsman.entity.Craftsman;
import com.lmf.heritage.module.craftsman.mapper.CraftsmanMapper;
import com.lmf.heritage.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 匠人身份解析
 *
 * <p><b>为什么单独抽一个组件</b>：匠人端有商品、课程、预约、评价、订单、看板六个模块，
 * 每个模块的第一步都是「拿到当前登录的匠人 id」。这段逻辑里有「账号未绑定匠人信息」
 * 这条业务判断，如果每个服务各写一份 private 方法，规则一旦变化（比如以后允许
 * 一个账号开多个匠人铺子）就要改六处，且很容易改漏。</p>
 *
 * <p><b>为什么是查询而不是从令牌里取</b>：令牌里只有 {@code userId} 与角色。
 * 匠人是独立的业务实体（有自己的资质审核状态、等级、结算信息），
 * 只有拿到 {@code craftsman.id} 才能正确圈定「是不是我的数据」。
 * 这个映射每次请求查一次，量很小，换来的是权限判断的单一事实来源。</p>
 *
 * @author 李明峰 23100025
 */
@Component
@RequiredArgsConstructor
public class CraftsmanHelper {

    private final CraftsmanMapper craftsmanMapper;

    /**
     * 取当前登录账号名下的匠人 id
     *
     * @throws BusinessException 账号未绑定匠人信息（2006 CRAFTSMAN_NOT_AUDITED）
     */
    public Long currentCraftsmanId() {
        return currentCraftsman().getId();
    }

    /**
     * 取当前登录账号名下的匠人实体
     *
     * <p>看板、回复评价这类场景除了 id 还要匠人姓名，直接复用同一次查询，
     * 避免为了拿个名字再跑一趟数据库。</p>
     *
     * <p><b>为什么要校验审核状态</b>：匠人资质是平台把关的入口，P8 的管理端审核只有
     * 在校验状态之后才真正有效——否则一个「待审核」甚至「已驳回」的申请者，
     * 只要他的账号角色是 2，就能发商品、开课程、核销签到，审核形同虚设。
     * 三种状态给三种文案，是因为用户需要知道下一步该做什么：
     * 待审核要等、已驳回要改材料重提，两者都不是「再等等就好了」。</p>
     */
    public Craftsman currentCraftsman() {
        Long userId = UserContext.userId();
        Craftsman craftsman = craftsmanMapper.selectOne(new LambdaQueryWrapper<Craftsman>()
                .eq(Craftsman::getUserId, userId)
                .last("LIMIT 1"));
        BusinessException.throwIf(craftsman == null,
                ErrorCode.CRAFTSMAN_NOT_AUDITED, "当前账号未绑定匠人信息");

        Integer auditStatus = craftsman.getAuditStatus();
        BusinessException.throwIf(Integer.valueOf(Constants.AUDIT_WAIT).equals(auditStatus),
                ErrorCode.CRAFTSMAN_NOT_AUDITED, "匠人资质正在审核中，通过后即可使用匠人端功能");
        BusinessException.throwIf(Integer.valueOf(Constants.AUDIT_REJECTED).equals(auditStatus),
                ErrorCode.CRAFTSMAN_NOT_AUDITED, "匠人资质审核未通过，请修改资料后重新提交");
        return craftsman;
    }

    /**
     * 断言某个匠人 id 就是当前登录匠人，否则抛「无权操作」
     *
     * <p>用于「先按 id 查到记录，再确认这条记录属于我」的横向越权防护。
     * 管理员账号放行，便于后台代运营。</p>
     */
    public void requireSelf(Long ownerCraftsmanId) {
        if (UserContext.isAdmin()) {
            return;
        }
        BusinessException.throwIf(!currentCraftsmanId().equals(ownerCraftsmanId),
                ErrorCode.NO_PERMISSION, "无权操作他人的数据");
    }
}
