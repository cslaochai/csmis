package com.mis.service;

import com.mis.dto.ChangePasswordDTO;
import com.mis.dto.LoginRequestDTO;
import com.mis.vo.LoginVO;
import com.mis.vo.MenuVO;
import com.mis.vo.PublicKeyVO;
import com.mis.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 认证服务：登录 / 当前用户信息 / 菜单 / 登出 / 改密
 */
public interface AuthService {

    /** 当前有效的口令加密公钥，供前端把明文口令加密后再上网 */
    PublicKeyVO publicKey();

    LoginVO login(LoginRequestDTO dto, HttpServletRequest request);

    UserInfoVO info();

    /** 当前用户可见菜单树（按当前角色，前端布局与路由构建） */
    List<MenuVO> menus();

    void logout();

    void changePassword(ChangePasswordDTO dto);
}
