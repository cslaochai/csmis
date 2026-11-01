package com.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mis.common.base.PageResult;
import com.mis.common.exception.BusinessException;
import com.mis.dto.PostItemDTO;
import com.mis.dto.ResetPasswordDTO;
import com.mis.dto.UserQueryPageDTO;
import com.mis.dto.UserStatusDTO;
import com.mis.dto.UserUpsertDTO;
import com.mis.entity.SysDepartment;
import com.mis.entity.SysEmployee;
import com.mis.entity.SysEmployeePost;
import com.mis.entity.SysRole;
import com.mis.entity.SysUser;
import com.mis.mapper.SysDepartmentMapper;
import com.mis.mapper.SysEmployeeMapper;
import com.mis.mapper.SysEmployeePostMapper;
import com.mis.mapper.SysRoleMapper;
import com.mis.mapper.SysUserMapper;
import com.mis.service.SystemPostService;
import com.mis.service.SystemUserService;
import com.mis.vo.PostVO;
import com.mis.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户管理实现（HIS 口径：院内用户自动建员工 + 岗位整单替换）
 */
@Service
@RequiredArgsConstructor
public class SystemUserServiceImpl implements SystemUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysEmployeeMapper employeeMapper;
    private final SysEmployeePostMapper employeePostMapper;
    private final SysRoleMapper roleMapper;
    private final SysDepartmentMapper departmentMapper;
    private final SystemPostService postService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> page(UserQueryPageDTO dto) {
        final String keyword = (dto.getKeyword() == null || dto.getKeyword().isEmpty())
                ? dto.getUserName()
                : dto.getKeyword();
        Page<SysUser> page = userMapper.selectPage(new Page<>(dto.getPage(), dto.getSize()),
                new LambdaQueryWrapper<SysUser>()
                        .and(keyword != null && !keyword.isEmpty(), w -> w
                                .like(SysUser::getUserName, keyword)
                                .or()
                                .like(SysUser::getRealName, keyword))
                        .eq(dto.getStatus() != null, SysUser::getStatus, dto.getStatus())
                        .orderByDesc(SysUser::getCreateTime));

        List<UserVO> vos = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(), vos);
    }

    @Override
    public UserVO detail(Long userId, Long id) {
        Long rid = userId != null ? userId : id;
        if (rid == null) {
            throw new BusinessException("用户ID不能为空");
        }
        return detail(rid);
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserVO vo = toVO(user);
        SysEmployee emp = user.getEmpId() == null ? null : employeeMapper.selectById(user.getEmpId());
        if (emp != null) {
            vo.setEmpNo(emp.getEmpCode());
            vo.setGender(emp.getGender());
            vo.setBirthDate(emp.getBirthDate());
            vo.setIdCard(emp.getIdCard());
            vo.setPhone(emp.getPhone());
            vo.setEmail(emp.getEmail());
            vo.setTitle(emp.getTitle());
            vo.setPosition(emp.getPosition());
        }
        vo.setPosts(postsOfEmployee(user.getEmpId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO upsert(UserUpsertDTO dto) {
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        String operator = operatorName();

        if (dto.getId() == null) {
            Long exist = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUserName, dto.getUserName()));
            if (exist != null && exist > 0) {
                throw new BusinessException("用户名已存在");
            }
            user.setPassword(passwordEncoder.encode(
                    dto.getPassword() == null || dto.getPassword().isEmpty() ? DEFAULT_PASSWORD : dto.getPassword()));
            user.setLoginCount(0);
            user.setCreateBy(operator);
            // 院内用户：自动建员工档案并关联
            if (isInternal(dto)) {
                SysEmployee emp = createEmployee(dto, operator);
                user.setEmpId(emp.getId());
            }
            userMapper.insert(user);
        } else {
            SysUser exist = userMapper.selectById(dto.getId());
            if (exist == null) {
                throw new BusinessException("用户不存在");
            }
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                user.setPasswordUpdateTime(LocalDateTime.now());
            } else {
                user.setPassword(null);
            }
            user.setUpdateBy(operator);
            userMapper.updateById(user);
            // 编辑：更新员工信息（院内用户）与岗位
            if (isInternal(dto)) {
                updateEmployee(dto, exist.getEmpId(), operator);
            }
        }

        // 岗位整单替换（角色 × 科室；posts 为 null 表示未提交不处理）
        if (dto.getPosts() != null && user.getEmpId() != null) {
            postService.replacePostsOfEmployee(user.getEmpId(), dto.getPosts(), operator);
        }
        return toVO(userMapper.selectById(user.getId()));
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        Long id = dto.resolveId();
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(
                dto.getPassword() == null || dto.getPassword().isEmpty() ? DEFAULT_PASSWORD : dto.getPassword()));
        update.setPasswordUpdateTime(LocalDateTime.now());
        update.setUpdateBy(operatorName());
        userMapper.updateById(update);
    }

    @Override
    public void changeStatus(UserStatusDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(dto.getId());
        update.setStatus(dto.getStatus());
        update.setUpdateBy(operatorName());
        userMapper.updateById(update);
    }

    @Override
    public void deleteById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    // ------------------------------------------------------------ 内部工具

    private boolean isInternal(UserUpsertDTO dto) {
        return dto.getUserType() == null || dto.getUserType() == 1;
    }

    /** 新增院内用户：建员工档案（工号自动生成、主岗位科室为默认科室） */
    private SysEmployee createEmployee(UserUpsertDTO dto, String operator) {
        SysEmployee emp = new SysEmployee();
        emp.setEmpCode(generateEmpCode());
        emp.setEmpName(dto.getRealName());
        emp.setEmpType(1);
        emp.setGender(dto.getGender());
        emp.setBirthDate(dto.getBirthDate());
        emp.setIdCard(dto.getIdCard());
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setTitle(dto.getTitle());
        emp.setPosition(dto.getPosition());
        Long deptId = primaryDeptId(dto);
        emp.setDeptId(deptId);
        emp.setDeptName(deptNameOf(deptId));
        emp.setStatus(1);
        emp.setCreateBy(operator);
        employeeMapper.insert(emp);
        return emp;
    }

    /** 编辑院内用户：更新员工档案字段（empId 为空则补建） */
    private void updateEmployee(UserUpsertDTO dto, Long empId, String operator) {
        SysEmployee emp = empId == null ? null : employeeMapper.selectById(empId);
        if (emp == null) {
            SysEmployee created = createEmployee(dto, operator);
            SysUser update = new SysUser();
            update.setId(dto.getId());
            update.setEmpId(created.getId());
            userMapper.updateById(update);
            return;
        }
        emp.setEmpName(dto.getRealName());
        if (dto.getGender() != null) emp.setGender(dto.getGender());
        if (dto.getBirthDate() != null) emp.setBirthDate(dto.getBirthDate());
        if (dto.getIdCard() != null) emp.setIdCard(dto.getIdCard());
        if (dto.getPhone() != null) emp.setPhone(dto.getPhone());
        if (dto.getEmail() != null) emp.setEmail(dto.getEmail());
        if (dto.getTitle() != null) emp.setTitle(dto.getTitle());
        if (dto.getPosition() != null) emp.setPosition(dto.getPosition());
        Long deptId = primaryDeptId(dto);
        if (deptId != null) {
            emp.setDeptId(deptId);
            emp.setDeptName(deptNameOf(deptId));
        }
        emp.setUpdateBy(operator);
        employeeMapper.updateById(emp);
    }

    private Long primaryDeptId(UserUpsertDTO dto) {
        if (dto.getPosts() == null || dto.getPosts().isEmpty()) {
            return null;
        }
        return dto.getPosts().stream()
                .filter(p -> p.getDeptId() != null && Objects.equals(p.getIsPrimary(), 1))
                .map(PostItemDTO::getDeptId)
                .findFirst()
                .orElse(dto.getPosts().stream()
                        .map(PostItemDTO::getDeptId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    private String deptNameOf(Long deptId) {
        if (deptId == null) return null;
        SysDepartment dept = departmentMapper.selectById(deptId);
        return dept == null ? null : dept.getDeptName();
    }

    private String generateEmpCode() {
        long max = employeeMapper.selectCount(new LambdaQueryWrapper<>());
        String code;
        do {
            code = "EMP" + String.format("%04d", ++max);
        } while (employeeMapper.selectCount(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getEmpCode, code)) > 0);
        return code;
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        SysEmployee emp = user.getEmpId() == null ? null : employeeMapper.selectById(user.getEmpId());
        if (emp != null) {
            vo.setEmpName(emp.getEmpName());
            vo.setEmpNo(emp.getEmpCode());
            vo.setDeptId(emp.getDeptId());
            vo.setDeptName(emp.getDeptName());
        }
        List<SysRole> roles = rolesOfEmployee(user.getEmpId());
        vo.setRoleCodes(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        return vo;
    }

    private List<PostVO> postsOfEmployee(Long employeeId) {
        if (employeeId == null) {
            return new ArrayList<>();
        }
        List<SysEmployeePost> posts = employeePostMapper.selectList(new LambdaQueryWrapper<SysEmployeePost>()
                .eq(SysEmployeePost::getEmployeeId, employeeId));
        return posts.stream().map(this::toPostVO).collect(Collectors.toList());
    }

    private PostVO toPostVO(SysEmployeePost post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setEmployeeId(post.getEmployeeId());
        vo.setRoleId(post.getRoleId());
        vo.setDeptId(post.getDeptId());
        vo.setIsPrimary(post.getIsPrimary());
        vo.setEffectiveDate(post.getEffectiveDate());
        vo.setExpireDate(post.getExpireDate());
        SysRole role = roleMapper.selectById(post.getRoleId());
        if (role != null) {
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
        }
        SysDepartment dept = departmentMapper.selectById(post.getDeptId());
        if (dept != null) {
            vo.setDeptName(dept.getDeptName());
        }
        SysEmployee emp = employeeMapper.selectById(post.getEmployeeId());
        if (emp != null) {
            vo.setEmpCode(emp.getEmpCode());
            vo.setEmpName(emp.getEmpName());
        }
        return vo;
    }

    private List<SysRole> rolesOfEmployee(Long employeeId) {
        if (employeeId == null) {
            return new ArrayList<>();
        }
        List<Long> roleIds = employeePostMapper.selectList(new LambdaQueryWrapper<SysEmployeePost>()
                        .eq(SysEmployeePost::getEmployeeId, employeeId))
                .stream().map(SysEmployeePost::getRoleId).distinct().collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    private String operatorName() {
        com.mis.config.LoginUser loginUser = com.mis.config.UserContext.get();
        return loginUser == null ? "system" : loginUser.getUsername();
    }
}
