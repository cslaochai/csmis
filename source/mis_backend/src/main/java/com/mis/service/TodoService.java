package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.TodoHandleDTO;
import com.mis.dto.TodoQueryDTO;
import com.mis.vo.ActionVO;
import com.mis.vo.TodoVO;

import java.util.List;

/**
 * 待办任务（跨模块待办中心）
 */
public interface TodoService {

    PageVO<TodoVO> page(TodoQueryDTO q);

    /** 我的待办数量统计 */
    java.util.Map<String, Long> countByStatus();

    /** 处理待办（完成/取消） */
    ActionVO handle(TodoHandleDTO dto);

    /**
     * 推送待办（供各业务模块静态调用）。
     *
     * @param todoType   待办类型（对应业务表名，如 mis_fee_audit）
     * @param title      待办标题
     * @param bizType    业务类型
     * @param bizId      业务主键
     * @param bizNo      业务编号
     * @param assignee   处理人姓名
     * @param assigneeId 处理人ID
     * @param priority   优先级（1-高 2-中 3-低）
     * @return 待办ID，未生成返回 null
     */
    Long push(String todoType, String title, String bizType, Long bizId, String bizNo,
              String assignee, Long assigneeId, Integer priority);

    /** 按业务对象关闭未完成待办（业务办结时调用） */
    int closeByBiz(String bizType, Long bizId, String opinion);
}
