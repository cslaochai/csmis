package com.mis.service;

import com.mis.common.base.PageVO;
import com.mis.dto.OperLogQueryDTO;
import com.mis.vo.OperLogStatVO;
import com.mis.vo.OperLogVO;

/**
 * 操作日志（审计留痕）
 */
public interface OperLogService {

    PageVO<OperLogVO> page(OperLogQueryDTO q);

    /** 写入一条操作日志（由拦截器调用，失败不影响主流程） */
    void save(OperLogVO log);

    /** 操作日志统计（按模块/类型聚合，用于监管成效看板） */
    OperLogStatVO stat(Integer days);

    /** 清理指定天数之前的日志 */
    int clean(Integer keepDays);
}
