package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.dto.EmployeeQueryPageDTO;
import com.mis.dto.EmployeeUpsertDTO;
import com.mis.dto.QualificationBatchUpsertDTO;
import com.mis.vo.EmployeeVO;
import com.mis.vo.QualificationVO;

import java.util.List;

/**
 * 员工管理（HIS 口径：listPage / getById / employeeUpsert / deleteById + 资格证书）
 */
public interface SystemEmployeeService {

    PageResult<EmployeeVO> page(EmployeeQueryPageDTO dto);

    EmployeeVO detail(Long id);

    EmployeeVO upsert(EmployeeUpsertDTO dto);

    void deleteById(Long id);

    /** 某员工的资格证书列表 */
    List<QualificationVO> qualificationList(Long employeeId);

    /** 批量保存资格证书（新增 + 更新，事务内） */
    void qualificationBatchUpsert(QualificationBatchUpsertDTO dto);

    void qualificationDelete(Long id);
}
