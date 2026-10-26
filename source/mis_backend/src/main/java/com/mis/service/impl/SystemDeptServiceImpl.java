package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mis.common.exception.BusinessException;
import com.mis.config.LoginUser;
import com.mis.config.UserContext;
import com.mis.dto.DepartmentUpsertDTO;
import com.mis.entity.MssDepartment;
import com.mis.mapper.MssDepartmentMapper;
import com.mis.service.SystemDeptService;
import com.mis.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 经办科室（树形 + CRUD，HIS 口径）
 */
@Service
@RequiredArgsConstructor
public class SystemDeptServiceImpl implements SystemDeptService {

    private final MssDepartmentMapper departmentMapper;

    @Override
    public List<DepartmentVO> selectList() {
        return departmentMapper.selectList(new LambdaQueryWrapper<MssDepartment>()
                        .eq(MssDepartment::getStatus, 1)
                        .orderByAsc(MssDepartment::getSortOrder)
                        .orderByAsc(MssDepartment::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<DepartmentVO> tree() {
        List<MssDepartment> all = departmentMapper.selectList(new LambdaQueryWrapper<MssDepartment>()
                .orderByAsc(MssDepartment::getSortOrder)
                .orderByAsc(MssDepartment::getId));
        List<DepartmentVO> roots = all.stream()
                .filter(d -> d.getParentId() == null || d.getParentId() == 0)
                .map(this::toVO)
                .collect(Collectors.toList());
        for (DepartmentVO root : roots) {
            fillChildren(root, all);
        }
        return roots;
    }

    private void fillChildren(DepartmentVO parent, List<MssDepartment> all) {
        List<DepartmentVO> children = all.stream()
                .filter(d -> Objects.equals(d.getParentId(), parent.getId()))
                .map(this::toVO)
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (DepartmentVO child : children) {
            fillChildren(child, all);
        }
    }

    @Override
    public DepartmentVO getById(Long deptId, Long id) {
        Long rid = deptId != null ? deptId : id;
        if (rid == null) {
            throw new BusinessException("部门ID不能为空");
        }
        return getById(rid);
    }

    @Override
    public DepartmentVO getById(Long id) {
        MssDepartment dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return toVO(dept);
    }

    @Override
    public DepartmentVO upsert(DepartmentUpsertDTO dto) {
        MssDepartment dept = new MssDepartment();
        BeanUtils.copyProperties(dto, dept);
        String operator = operatorName();

        if (dto.getId() == null) {
            if (dto.getDeptCode() == null || dto.getDeptCode().isEmpty()) {
                dept.setDeptCode(generateDeptCode());
            }
            dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dept.setCreateBy(operator);
            departmentMapper.insert(dept);
        } else {
            MssDepartment exist = departmentMapper.selectById(dto.getId());
            if (exist == null) {
                throw new BusinessException("部门不存在");
            }
            dept.setId(dto.getId());
            dept.setUpdateBy(operator);
            departmentMapper.updateById(dept);
        }
        return toVO(dept);
    }

    private DepartmentVO toVO(MssDepartment dept) {
        DepartmentVO vo = new DepartmentVO();
        BeanUtils.copyProperties(dept, vo);
        return vo;
    }

    @Override
    public void deleteById(Long id) {
        MssDepartment dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        Long childCount = departmentMapper.selectCount(new LambdaQueryWrapper<MssDepartment>()
                .eq(MssDepartment::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("该部门下有子部门，不能删除");
        }
        departmentMapper.deleteById(id);
    }

    private String generateDeptCode() {
        long max = departmentMapper.selectCount(new LambdaQueryWrapper<>());
        String code;
        do {
            code = String.format("%04d", ++max);
        } while (departmentMapper.selectCount(new LambdaQueryWrapper<MssDepartment>()
                .eq(MssDepartment::getDeptCode, code)) > 0);
        return code;
    }

    private String operatorName() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? "system" : loginUser.getUsername();
    }
}
