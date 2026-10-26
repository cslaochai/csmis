package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.dto.ResetPasswordDTO;
import com.mis.dto.UserQueryPageDTO;
import com.mis.dto.UserStatusDTO;
import com.mis.dto.UserUpsertDTO;
import com.mis.vo.UserVO;

/**
 * 用户管理
 */
public interface SystemUserService {

    PageResult<UserVO> page(UserQueryPageDTO dto);

    /** 详情（含员工字段与岗位行，HIS：getById） */
    UserVO detail(Long id);

    /** HIS 契约：/getById 兼容 userId 与 id 双主键入参，二选一，都缺省抛错 */
    UserVO detail(Long userId, Long id);

    UserVO upsert(UserUpsertDTO dto);

    void resetPassword(ResetPasswordDTO dto);

    void changeStatus(UserStatusDTO dto);

    void deleteById(Long id);
}
