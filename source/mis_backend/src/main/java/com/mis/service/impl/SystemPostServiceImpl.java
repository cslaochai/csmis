package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageResult;
import com.mis.common.exception.BusinessException;
import com.mis.config.LoginUser;
import com.mis.config.UserContext;
import com.mis.dto.PostItemDTO;
import com.mis.dto.PostReplaceDTO;
import com.mis.entity.SysDepartment;
import com.mis.entity.SysEmployee;
import com.mis.entity.SysEmployeePost;
import com.mis.entity.SysRole;
import com.mis.mapper.SysDepartmentMapper;
import com.mis.mapper.SysEmployeeMapper;
import com.mis.mapper.SysEmployeePostMapper;
import com.mis.mapper.SysRoleMapper;
import com.mis.service.SystemPostService;
import com.mis.vo.EmployeeSelectVO;
import com.mis.vo.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 员工岗位管理实现（整单替换，本表无 del_flag 走物理删）
 */
@Service
@RequiredArgsConstructor
public class SystemPostServiceImpl implements SystemPostService {

    private final SysEmployeePostMapper employeePostMapper;
    private final SysEmployeeMapper employeeMapper;
    private final SysRoleMapper roleMapper;
    private final SysDepartmentMapper departmentMapper;

    @Override
    public PageResult<PostVO> page(String keyword, long pageNum, long pageSize) {
        List<Long> empIds = null;
        if (keyword != null && !keyword.isEmpty()) {
            empIds = employeeMapper.selectList(new LambdaQueryWrapper<SysEmployee>()
                            .like(SysEmployee::getEmpName, keyword)
                            .or()
                            .like(SysEmployee::getEmpCode, keyword))
                    .stream().map(SysEmployee::getId).collect(Collectors.toList());
            if (empIds.isEmpty()) {
                return PageResult.of(0, pageNum, pageSize, 0, new ArrayList<>());
            }
        }
        Page<SysEmployeePost> page = employeePostMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysEmployeePost>()
                        .in(empIds != null && !empIds.isEmpty(), SysEmployeePost::getEmployeeId, empIds)
                        .orderByAsc(SysEmployeePost::getEmployeeId)
                        .orderByDesc(SysEmployeePost::getIsPrimary)
                        .orderByAsc(SysEmployeePost::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @Override
    public List<PostVO> postsOfEmployee(Long employeeId) {
        List<SysEmployeePost> posts = employeePostMapper.selectList(new LambdaQueryWrapper<SysEmployeePost>()
                .eq(SysEmployeePost::getEmployeeId, employeeId)
                .orderByDesc(SysEmployeePost::getIsPrimary)
                .orderByAsc(SysEmployeePost::getId));
        return posts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePosts(PostReplaceDTO dto) {
        SysEmployee employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        replacePostsOfEmployee(dto.getEmployeeId(), dto.getPosts(), operatorName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePostsOfEmployee(Long employeeId, List<PostItemDTO> posts, String operator) {
        // 本表无 del_flag：先物理删旧行再插入，避免占用唯一键
        employeePostMapper.delete(new LambdaQueryWrapper<SysEmployeePost>()
                .eq(SysEmployeePost::getEmployeeId, employeeId));

        if (posts == null || posts.isEmpty()) {
            return;
        }
        // 硬不变量：恰好一条主岗位；多条主岗位只认第一条，没有则把第一条提为主岗位
        boolean hasPrimary = posts.stream().anyMatch(p -> Objects.equals(p.getIsPrimary(), 1));
        for (int i = 0; i < posts.size(); i++) {
            PostItemDTO item = posts.get(i);
            Long roleId = resolveRoleId(item);
            if (roleId == null || item.getDeptId() == null) {
                continue;
            }
            SysEmployeePost post = new SysEmployeePost();
            post.setEmployeeId(employeeId);
            post.setRoleId(roleId);
            post.setDeptId(item.getDeptId());
            post.setIsPrimary(hasPrimary
                    ? (Objects.equals(item.getIsPrimary(), 1) ? 1 : 0)
                    : (i == 0 ? 1 : 0));
            post.setEffectiveDate(item.getEffectiveDate());
            post.setExpireDate(item.getExpireDate());
            post.setCreateBy(operator);
            employeePostMapper.insert(post);
        }
    }

    /** roleCode 优先（HIS 口径），否则回退 roleId */
    private Long resolveRoleId(PostItemDTO item) {
        if (item.getRoleCode() != null && !item.getRoleCode().isEmpty()) {
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, item.getRoleCode())
                    .last("LIMIT 1"));
            return role == null ? null : role.getId();
        }
        return item.getRoleId();
    }

    @Override
    public List<EmployeeSelectVO> employeeSelect(String keyword) {
        return employeeMapper.selectList(new LambdaQueryWrapper<SysEmployee>()
                        .eq(SysEmployee::getStatus, 1)
                        .and(keyword != null && !keyword.isEmpty(), w -> w
                                .like(SysEmployee::getEmpName, keyword)
                                .or()
                                .like(SysEmployee::getEmpCode, keyword))
                        .orderByAsc(SysEmployee::getId)
                        .last("LIMIT 50"))
                .stream().map(e -> {
                    EmployeeSelectVO vo = new EmployeeSelectVO();
                    vo.setId(e.getId());
                    vo.setEmpCode(e.getEmpCode());
                    vo.setEmpName(e.getEmpName());
                    vo.setDeptId(e.getDeptId());
                    vo.setDeptName(e.getDeptName());
                    vo.setStatus(e.getStatus());
                    return vo;
                }).collect(Collectors.toList());
    }

    // ------------------------------------------------------------ 内部工具

    private PostVO toVO(SysEmployeePost post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setEmployeeId(post.getEmployeeId());
        vo.setRoleId(post.getRoleId());
        vo.setDeptId(post.getDeptId());
        vo.setIsPrimary(post.getIsPrimary());
        vo.setEffectiveDate(post.getEffectiveDate());
        vo.setExpireDate(post.getExpireDate());

        SysEmployee emp = employeeMapper.selectById(post.getEmployeeId());
        if (emp != null) {
            vo.setEmpCode(emp.getEmpCode());
            vo.setEmpName(emp.getEmpName());
        }
        SysRole role = roleMapper.selectById(post.getRoleId());
        if (role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        SysDepartment dept = departmentMapper.selectById(post.getDeptId());
        if (dept != null) {
            vo.setDeptName(dept.getDeptName());
        }
        return vo;
    }

    private String operatorName() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? "system" : loginUser.getUsername();
    }
}
