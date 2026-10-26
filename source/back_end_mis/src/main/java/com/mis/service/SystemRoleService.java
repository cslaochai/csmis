package com.mis.service;

import com.mis.common.base.PageResult;
import com.mis.dto.RoleMenuSaveDTO;
import com.mis.dto.RoleUpsertDTO;
import com.mis.vo.RoleListVO;
import com.mis.vo.RoleVO;

import java.util.List;

/**
 * 角色管理
 */
public interface SystemRoleService {

    PageResult<RoleListVO> page(String keyword, long pageNum, long pageSize);

    List<RoleListVO> selectList();

    RoleVO getDetailById(Long id);

    /** HIS 契约：/getById 兼容 roleId 与 id 双主键入参，二选一，都缺省抛错 */
    RoleVO getDetailById(Long roleId, Long id);

    RoleVO upsert(RoleUpsertDTO dto);

    void deleteById(Long id);

    /** 角色已授权的菜单 ID（HIS：getMenuIds，字符串列表供树回显） */
    List<String> getMenuIds(Long roleId);

    /** 角色菜单授权整单替换（HIS：saveRoleMenu） */
    void saveRoleMenu(RoleMenuSaveDTO dto);
}
