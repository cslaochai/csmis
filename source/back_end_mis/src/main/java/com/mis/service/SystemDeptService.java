package com.mis.service;

import com.mis.dto.DepartmentUpsertDTO;
import com.mis.vo.DepartmentVO;

import java.util.List;

/**
 * 经办科室（岗位配置下拉 + HIS 契约 /system/department/**）
 */
public interface SystemDeptService {

    List<DepartmentVO> selectList();

    /** 科室树（HIS 口径：/system/department/tree，按 parent_id 组装） */
    List<DepartmentVO> tree();

    DepartmentVO getById(Long id);

    /** HIS 契约：/getById 兼容 deptId 与 id 双主键入参，二选一，都缺省抛错 */
    DepartmentVO getById(Long deptId, Long id);

    DepartmentVO upsert(DepartmentUpsertDTO dto);

    void deleteById(Long id);
}
