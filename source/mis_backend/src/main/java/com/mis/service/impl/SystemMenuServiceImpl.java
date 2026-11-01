package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.common.exception.BusinessException;
import com.mis.config.LoginUser;
import com.mis.config.UserContext;
import com.mis.dto.MenuUpsertDTO;
import com.mis.entity.SysMenu;
import com.mis.entity.SysRoleMenu;
import com.mis.mapper.SysMenuMapper;
import com.mis.mapper.SysRoleMenuMapper;
import com.mis.service.SystemMenuService;
import com.mis.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜单管理实现（树形）
 */
@Service
@RequiredArgsConstructor
public class SystemMenuServiceImpl implements SystemMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuVO> tree() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        List<MenuVO> vos = menus.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(vos);
    }

    @Override
    public MenuVO getById(Long menuId, Long id) {
        Long rid = menuId != null ? menuId : id;
        if (rid == null) {
            throw new BusinessException("菜单ID不能为空");
        }
        return getById(rid);
    }

    @Override
    public MenuVO getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return toVO(menu);
    }

    @Override
    public MenuVO upsert(MenuUpsertDTO dto) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        String operator = operatorName();

        if (dto.getId() == null) {
            if (dto.getMenuKey() != null && !dto.getMenuKey().isEmpty()) {
                Long exist = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getMenuKey, dto.getMenuKey()));
                if (exist != null && exist > 0) {
                    throw new BusinessException("菜单标识已存在");
                }
            }
            menu.setCreateBy(operator);
            menuMapper.insert(menu);
        } else {
            if (menuMapper.selectById(dto.getId()) == null) {
                throw new BusinessException("菜单不存在");
            }
            menu.setUpdateBy(operator);
            menuMapper.updateById(menu);
        }
        return toVO(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        // 目录删除连带子菜单；子菜单删除连带角色授权
        List<Long> ids = new ArrayList<>();
        collectTreeIds(id, ids);
        ids.add(id);
        menuMapper.deleteBatchIds(ids);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, ids));
    }

    private void collectTreeIds(Long parentId, List<Long> out) {
        List<SysMenu> children = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId));
        for (SysMenu child : children) {
            out.add(child.getId());
            collectTreeIds(child.getId(), out);
        }
    }

    private MenuVO toVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    private List<MenuVO> buildTree(List<MenuVO> menus) {
        List<MenuVO> tree = new ArrayList<>();
        for (MenuVO m : menus) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                tree.add(m);
            }
        }
        for (MenuVO root : tree) {
            fillChildren(root, menus);
        }
        return tree;
    }

    private void fillChildren(MenuVO parent, List<MenuVO> menus) {
        List<MenuVO> children = menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parent.getId()))
                .sorted(Comparator.comparing(m -> m.getSortOrder() == null ? 0 : m.getSortOrder()))
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (MenuVO child : children) {
            fillChildren(child, menus);
        }
    }

    private String operatorName() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? "system" : loginUser.getUsername();
    }
}
