package com.mis.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 结算清单查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SettleListQueryDTO extends BizPageParam implements Serializable {

    /** 清单号 */
    private String listNo;

    /** 结算单号 */
    private String billNo;

    /** 定点机构编码 */
    private String medinsCode;

    /** 患者姓名 */
    private String patientName;

    /** 险种 */
    private String insuranceType;

    /** 就诊类型 */
    private String encounterType;

    /** 质控状态（0-未质控 1-质控通过 2-质控异常 3-已作废） */
    private Integer checkStatus;

    /** 上传批次号 */
    private String uploadBatchNo;
}
