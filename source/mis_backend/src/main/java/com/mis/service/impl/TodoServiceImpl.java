package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageVO;
import com.mis.dto.TodoHandleDTO;
import com.mis.dto.TodoQueryDTO;
import com.mis.entity.SysTodo;
import com.mis.mapper.SysTodoMapper;
import com.mis.service.TodoService;
import com.mis.vo.ActionVO;
import com.mis.vo.TodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final SysTodoMapper todoMapper;

    /** 静态门面：业务模块可直接调用，无需注入本 Service */
    private static TodoService facade;

    /**
     * 由 Spring 注入自身引用（用 ObjectProvider 避免构造期循环依赖）。
     * 用法：TodoServiceImpl.pushTodo(...) —— 业务模块零注入成本。
     */
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    public void setFacade(TodoService self) {
        facade = self;
    }

    /**
     * 静态推待办入口（供各业务模块调用）。
     * 失败不抛异常——待办是辅助能力，不能因为它拖垮主业务。
     */
    public static Long pushTodo(String todoType, String title, String bizType, Long bizId, String bizNo,
                                String assignee, Long assigneeId, Integer priority) {
        try {
            if (facade == null) {
                return null;
            }
            return facade.push(todoType, title, bizType, bizId, bizNo, assignee, assigneeId, priority);
        } catch (Exception ex) {
            log.warn("[待办] 推送失败（不影响主流程）：{} - {}", title, ex.getMessage());
            return null;
        }
    }

    /**
     * 静态关待办入口（业务办结时调用）。
     */
    public static int closeTodo(String bizType, Long bizId, String opinion) {
        try {
            if (facade == null) {
                return 0;
            }
            return facade.closeByBiz(bizType, bizId, opinion);
        } catch (Exception ex) {
            log.warn("[待办] 关闭失败（不影响主流程）：{}", ex.getMessage());
            return 0;
        }
    }

    @Override
    public Long push(String todoType, String title, String bizType, Long bizId, String bizNo,
                     String assignee, Long assigneeId, Integer priority) {
        // 同业务对象 + 同类型已有未完成待办则不重复推送
        if (bizType != null && bizId != null) {
            Long exists = todoMapper.selectCount(new LambdaQueryWrapper<SysTodo>()
                    .eq(SysTodo::getBizType, bizType)
                    .eq(SysTodo::getBizId, bizId)
                    .eq(SysTodo::getTodoType, todoType)
                    .in(SysTodo::getStatus, 0, 1));
            if (exists != null && exists > 0) {
                return null;
            }
        }
        SysTodo t = new SysTodo();
        t.setTodoType(todoType);
        t.setTodoTitle(title);
        t.setBizType(bizType);
        t.setBizId(bizId);
        t.setBizNo(bizNo);
        t.setAssignee(assignee);
        t.setAssigneeId(assigneeId);
        t.setPriority(priority == null ? 2 : priority);
        t.setStatus(0);
        todoMapper.insert(t);
        return t.getId();
    }

    @Override
    public int closeByBiz(String bizType, Long bizId, String opinion) {
        if (bizType == null || bizId == null) {
            return 0;
        }
        List<SysTodo> list = todoMapper.selectList(new LambdaQueryWrapper<SysTodo>()
                .eq(SysTodo::getBizType, bizType)
                .eq(SysTodo::getBizId, bizId)
                .in(SysTodo::getStatus, 0, 1));
        for (SysTodo t : list) {
            t.setStatus(2);
            t.setFinishTime(LocalDateTime.now());
            t.setFinishOpinion(opinion);
            todoMapper.updateById(t);
        }
        return list.size();
    }

    @Override
    public PageVO<TodoVO> page(TodoQueryDTO q) {
        LambdaQueryWrapper<SysTodo> mw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getTodoType())) {
            mw.eq(SysTodo::getTodoType, q.getTodoType());
        }
        if (StringUtils.hasText(q.getBizType())) {
            mw.eq(SysTodo::getBizType, q.getBizType());
        }
        if (StringUtils.hasText(q.getAssignee())) {
            mw.like(SysTodo::getAssignee, q.getAssignee());
        }
        if (q.getPriority() != null) {
            mw.eq(SysTodo::getPriority, q.getPriority());
        }
        if (q.getStatus() != null) {
            mw.eq(SysTodo::getStatus, q.getStatus());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            mw.and(w -> w.like(SysTodo::getTodoTitle, q.getKeyword())
                    .or().like(SysTodo::getBizNo, q.getKeyword()));
        }
        mw.orderByAsc(SysTodo::getPriority).orderByDesc(SysTodo::getCreateTime);

        IPage<SysTodo> pg = todoMapper.selectPage(new Page<>(q.getPage(), q.getSize()), mw);
        List<TodoVO> list = new ArrayList<>();
        for (SysTodo e : pg.getRecords()) {
            list.add(toVO(e));
        }
        return PageVO.of(list, pg.getTotal());
    }

    @Override
    public Map<String, Long> countByStatus() {
        Map<String, Long> m = new HashMap<>();
        m.put("待处理", 0L);
        m.put("处理中", 0L);
        m.put("已完成", 0L);
        m.put("已取消", 0L);
        for (SysTodo t : todoMapper.selectList(new LambdaQueryWrapper<SysTodo>()
                .select(SysTodo::getStatus))) {
            String k = switch (t.getStatus() == null ? 0 : t.getStatus()) {
                case 1 -> "处理中";
                case 2 -> "已完成";
                case 3 -> "已取消";
                default -> "待处理";
            };
            m.merge(k, 1L, Long::sum);
        }
        return m;
    }

    @Override
    public ActionVO handle(TodoHandleDTO dto) {
        SysTodo t = todoMapper.selectById(dto.getId());
        if (t == null) {
            return ActionVO.fail("待办不存在");
        }
        if (t.getStatus() != null && (t.getStatus() == 2 || t.getStatus() == 3)) {
            return ActionVO.fail("该待办已处理，不能重复操作");
        }
        t.setStatus(dto.getStatus());
        t.setFinishTime(LocalDateTime.now());
        t.setFinishOpinion(dto.getFinishOpinion());
        todoMapper.updateById(t);
        return ActionVO.ok();
    }

    private TodoVO toVO(SysTodo e) {
        TodoVO vo = new TodoVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
