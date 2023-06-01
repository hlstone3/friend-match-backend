package com.yupi.yupao.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author hongs
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2023-03-04 20:47:54
 */
public interface UserService extends IService<User> {
    /**
     * 注册
     *
     * @param userAccount   用户账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @param planetCode    星球编码
     * @return 用户id
     */
    BaseResponse<Long> userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HttpServletRequest
     * @return 返回用户脱敏的信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 通过用户名查询
     *
     * @param userAccount 用户名
     * @return 符合条件的用户集合
     */
    List<User> userSearch(String userAccount, HttpServletRequest request);


    /**
     * 通过用户id删除用户
     *
     * @param id 用户id
     * @return 是否成功
     */
    Boolean userDelete(long id, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser 原始用户
     * @return 脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 获取当前用户
     *
     * @return 当前用户
     */
    User getCurrentUser(HttpServletRequest request);

    /**
     * 用户退出
     */
    BaseResponse<Integer> userLogout(HttpServletRequest request);

    /**
     * 通过标签名查询用户
     *
     * @param tagNameList 标签
     * @return 用户列表
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 修改用户通过用户id
     *
     * @param editUser 要编辑的用户
     * @return 修改后的用户信息
     */
    Boolean updateUser(User editUser, HttpServletRequest request);

    /**
     * 当前用户是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 当前用户是否为管理员
     */
    boolean isAdmin(User loginUser);

    /**
     * 首页用户列表
     */
    Page<User> indexUserList(long pageNum, long pageSize, HttpServletRequest request);

    List<User> matchUsers(long num,User loginUser);


}
