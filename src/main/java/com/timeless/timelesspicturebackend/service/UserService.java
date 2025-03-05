package com.timeless.timelesspicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.timeless.timelesspicturebackend.model.dto.user.UserQueryRequest;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.timeless.timelesspicturebackend.model.vo.LoginUserVO;
import com.timeless.timelesspicturebackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author undefined
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-01-18 15:19:19
 */
public interface UserService extends IService<User> {
    /**
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      请求
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * @param password 密码
     * @return 加密后的密码
     */
    String getEncryptedPassword(String password);

    /**
     * @param user 用户
     * @return 登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取登录用户信息
     * @param request 请求
     * @return 登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**用户退出登录
     * @param request 请求
     * @return true/false
     */
    boolean userLogout(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    String uploadUserAvatar(MultipartFile multipartFile, User loginUser);
}
