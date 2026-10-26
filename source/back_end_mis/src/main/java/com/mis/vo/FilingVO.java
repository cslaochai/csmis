package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 异地就医备案出参：备案列表行。
 */
@Data
public class FilingVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private String idCardMask;
    private String insuranceArea;
    private String hospital;
    private String reason;
    private String fromArea;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validEnd;
    /** 状态（已备案/审核中/已失效） */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
