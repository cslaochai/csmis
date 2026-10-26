package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.dto.PersonQueryDTO;
import com.mis.dto.PersonSaveDTO;
import com.mis.vo.InsuredPersonVO;

public interface InsuredPersonService {

    /** 参保查询（按身份证/医保卡号/姓名） */
    InsuredPersonVO query(PersonQueryDTO dto);

    /** 管理端分页 */
    PageResult<InsuredPersonVO> page(String keyword, int pageNum, int pageSize);

    /** 新增/同步参保人（按身份证幂等） */
    InsuredPersonVO save(PersonSaveDTO dto);
}
