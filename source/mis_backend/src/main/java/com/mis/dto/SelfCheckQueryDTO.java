package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SelfCheckQueryDTO extends BizPageParam {

    private String keyword;
    private String medinsCode;
    /** 状态（1-待自查 2-自查中 3-已上报 4-已整改 5-已办结） */
    private Integer status;
}
