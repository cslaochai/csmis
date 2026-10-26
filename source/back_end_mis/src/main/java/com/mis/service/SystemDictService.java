package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.dto.DictDataUpsertDTO;
import com.mis.dto.DictTypeUpsertDTO;
import com.mis.vo.DictDataVO;
import com.mis.vo.DictGroupVO;
import com.mis.vo.DictTypeVO;

import java.util.List;

/**
 * 字典管理
 */
public interface SystemDictService {

    PageResult<DictTypeVO> typePage(String keyword, long pageNum, long pageSize);

    List<DictTypeVO> typeSelectList();

    DictTypeVO typeGetById(Long id);

    /** HIS 契约：/type/getById 兼容 typeId 与 id 双主键入参，二选一，都缺省抛错 */
    DictTypeVO typeGetById(Long typeId, Long id);

    DictTypeVO typeUpsert(DictTypeUpsertDTO dto);

    void typeDelete(Long id);

    PageResult<DictDataVO> dataPage(String dictType, String keyword, long pageNum, long pageSize);

    DictDataVO dataGetById(Long id);

    /** HIS 契约：/data/detail/getById 兼容 dataId 与 id 双主键入参，二选一，都缺省抛错 */
    DictDataVO dataGetById(Long dataId, Long id);

    /** 单值/逗号分隔多类型 → 数据列表（HIS：data/selectList） */
    List<DictDataVO> dataSelectList(String dictTypes);

    /** 单值/逗号分隔多类型 → 分组 [{dictType, dataList}]（HIS：data/selectGroup） */
    List<DictGroupVO> dataSelectGroup(String dictTypes);

    DictDataVO dataUpsert(DictDataUpsertDTO dto);

    void dataDelete(Long id);

    List<DictDataVO> dataList(String dictType);
}
