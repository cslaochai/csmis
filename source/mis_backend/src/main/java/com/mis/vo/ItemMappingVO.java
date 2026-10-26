package com.mis.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 院内项目 ↔ 医保目录对照出参：对照列表行。
 */
@Data
public class ItemMappingVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String hospItemCode;
    private String hospItemName;
    private String category;
    private String ybCode;
    private String ybName;
    private String payClass;
    /** 是否已对照 */
    private Boolean mapped;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
