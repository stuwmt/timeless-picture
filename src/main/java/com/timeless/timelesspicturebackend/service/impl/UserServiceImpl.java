package com.timeless.timelesspicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.timeless.timelesspicturebackend.auth.StpKit;
import com.timeless.timelesspicturebackend.constant.UserConstant;
import com.timeless.timelesspicturebackend.exception.BusinessException;
import com.timeless.timelesspicturebackend.exception.ErrorCode;
import com.timeless.timelesspicturebackend.exception.ThrowUtils;
import com.timeless.timelesspicturebackend.manager.upload.PictureUploadTemplate;
import com.timeless.timelesspicturebackend.model.dto.file.UploadPictureResult;
import com.timeless.timelesspicturebackend.model.dto.user.UserQueryRequest;
import com.timeless.timelesspicturebackend.model.entity.User;
import com.timeless.timelesspicturebackend.model.enums.UserRoleEnum;
import com.timeless.timelesspicturebackend.model.vo.LoginUserVO;
import com.timeless.timelesspicturebackend.model.vo.UserVO;
import com.timeless.timelesspicturebackend.service.UserService;
import com.timeless.timelesspicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author undefined
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-01-18 15:19:19
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private PictureUploadTemplate filePictureUpload;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR,
                "参数不能为空");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");
        ThrowUtils.throwIf(userAccount.length() < 6 || userAccount.length() > 20, ErrorCode.PARAMS_ERROR,
                "账号长度必须在6-20位之间");
        ThrowUtils.throwIf(userPassword.length() < 6 || userPassword.length() > 20, ErrorCode.PARAMS_ERROR, "密码长度必须在6" +
                "-20位之间");
        // 2.查询用户是否存在
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(user != null, ErrorCode.PARAMS_ERROR, "用户已存在");
        // 3.密码加密
        String encryptedPassword = getEncryptedPassword(userPassword);
        // 4.注册用户
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(userPassword);
        newUser.setUserRole(UserRoleEnum.USER.getValue());
        newUser.setUserName("无名");
        newUser.setUserPassword(encryptedPassword);
        boolean save = this.save(newUser);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "注册失败");
        return newUser.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数不能为空");
        // 2.查询用户是否存在
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        // 3.密码校验
        String encryptedPassword = getEncryptedPassword(userPassword);
        ThrowUtils.throwIf(!user.getUserPassword().equals(encryptedPassword), ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        // 4.保存登录信息
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到sa-token
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public String getEncryptedPassword(String password) {
        final String salt = "timeless";
        return DigestUtil.md5Hex(salt + password);
    }

    /**
     * @param user 用户
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (null == user) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(null == currentUser || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 重新查询用户信息
        User user = this.getById(currentUser.getId());
        ThrowUtils.throwIf(null == user, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return user;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(null == userObj, ErrorCode.OPERATION_ERROR);
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public String uploadUserAvatar(MultipartFile multipartFile, User loginUser) {
        ThrowUtils.throwIf(null == loginUser, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(multipartFile, uploadPathPrefix);
        return uploadPictureResult.getUrl();
    }
}




