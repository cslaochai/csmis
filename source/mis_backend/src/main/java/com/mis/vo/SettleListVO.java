package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算清单返回对象
 */
@Data
public class SettleListVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 清单号 */
    private String listNo;

    /** 结算单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long billId;

    /** 结算单号 */
    private String billNo;

    /** 定点机构编码 */
    private String medinsCode;

    /** 定点机构名称 */
    private String medinsName;

    /** 患者姓名 */
    private String patientName;

    /** 身份证掩码 */
    private String idCardMask;

    /** 险种 */
    private String insuranceType;

    /** 就诊类型 */
    private String encounterType;

    /** 项目数 */
    private Integer itemCount;

    /** 医疗总费用 */
    private BigDecimal totalAmount;

    /** 基金支付 */
    private BigDecimal insurancePay;

    /** 清单版本 */
    private String listVersion;

    /** 上传批次号 */
    private String uploadBatchNo;

    /** 质控状态（0-未质控 1-通过 2-异常 3-已作废） */
    private Integer checkStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
