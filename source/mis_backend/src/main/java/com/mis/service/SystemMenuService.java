package com.mis.service;

import com.mis.dto.MenuUpsertDTO;
import com.mis.vo.MenuVO;

import java.util.List;

/**
 * 菜单管理
 */
public interface SystemMenuService {

    List<MenuVO> tree();

    MenuVO getById(Long id);

    /** HIS 契约：/getById 兼容 menuId 与 id 双主键入参，二选一，都缺省抛错 */
    MenuVO getById(Long menuId, Long id);

    MenuVO upsert(MenuUpsertDTO dto);

    void deleteById(Long id);
}
