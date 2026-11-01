package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日拨付批次分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DailyQueryDTO extends BizPageParam {

    /** 业务日期（yyyy-MM-dd） */
    private String bizDate;
    /** 回单状态（0-待拨付 1-已回单） */
    private Integer remitStatus;
}
