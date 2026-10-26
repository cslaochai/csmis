package com.mis.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 字典分组出参（HIS：selectGroup，[{dictType, dataList}]）
 */
@Data
public class DictGroupVO implements Serializable {

    private String dictType;

    private List<DictDataVO> dataList;

    public DictGroupVO() {
    }

    public DictGroupVO(String dictType, List<DictDataVO> dataList) {
        this.dictType = dictType;
        this.dataList = dataList;
    }
}
