package com.yupi.yupao.controller;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.request.UserDeleteRequest;
import com.yupi.yupao.model.request.UserLoginRequest;
import com.yupi.yupao.model.request.UserRegisterRequest;
import com.yupi.yupao.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://127.0.0.1:5173"}, allowCredentials = "true")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册请求体
     * @return 返回用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> registerController(@RequestBody UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyEmpty(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
    }

    /**
     * 用户登录接口
     *
     * @param userLoginRequest 用户登录请求体
     * @param request          HttpServletRequest
     * @return 用户信息
     */
    @PostMapping("/login")
    public BaseResponse<User> loginController(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();


        if (StringUtils.isAnyEmpty(userAccount, userPassword)) {
            return null;
        }


        return ResultUtils.success(userService.userLogin(userAccount, userPassword, request));
    }

    /**
     * 通过用户名查询用户接口
     *
     * @param userAccount 用户名
     * @return 所有符合条件的用户集合
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchController(String userAccount, HttpServletRequest request) {
        return ResultUtils.success(userService.userSearch(userAccount, request));
    }

    /**
     * @param userDeleteRequest 用户删除请求体
     * @return 是否删除
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteController(@RequestBody UserDeleteRequest userDeleteRequest, HttpServletRequest request) {
        return ResultUtils.success(userService.userDelete(userDeleteRequest.getId(), request));
    }

    /**
     * 获取当前用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        return ResultUtils.success(currentUser);
    }

    /**
     * 用户退出登录
     */
    @GetMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        return userService.userLogout(request);
    }


    /**
     * 搜索用户通过标签
     *
     * @param tagNameList 标签列表
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList) {
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }


    /**
     * 更新用户
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody User editUser, HttpServletRequest request) {
        if (editUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userService.updateUser(editUser, request));
    }

    @GetMapping("/index")
    public BaseResponse<Page<User>> indexUserList(long pageNum, long pageSize, HttpServletRequest request) {
        return ResultUtils.success(userService.indexUserList(pageNum, pageSize, request));
    }

    @GetMapping("/match/index")
    public BaseResponse<List<User>> indexMatchUserList(long num, HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        List<User> userList = userService.matchUsers(num, currentUser);
        return ResultUtils.success(userList);
    }


}










































