package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.mapper.UserMapper;
import com.yupi.yupao.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.yupao.constant.Constant.ADMIN_ROLE;
import static com.yupi.yupao.constant.Constant.USER_LOGIN_STATE;

/**
 * @author hongs
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-03-04 20:47:54
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 盐，使密码更安全
     */
    private static final String SALT = "stone";

    /**
     * 注册
     *
     * @param userAccount   用户账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @param planetCode    星球编码
     * @return 用户id
     */
    @Override
    public BaseResponse<Long> userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.非空
        if (StringUtils.isAnyEmpty(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //2.账号长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3.密码不小于8位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //4.星球编码长度不能大于5位
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //账号不能包含特殊字符
        String validPattern = "\\pP|\\pS|\\s+";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        //星球编码不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编码已存在");
        }


        //密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.UNKNOWN_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HttpServletRequest
     * @return 返回用户脱敏的信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        //1.非空
        if (StringUtils.isAnyEmpty(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //2.账号长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //3.密码不小于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //4.账号不能包含特殊字符
        String validPattern = "\\pP|\\pS|\\s+";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            log.info("user login failed, userAccount Cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }

        //脱敏
        User safetyUser = getSafetyUser(user);

        //将用户信息保存到session中
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 通过用户名查询
     *
     * @param userAccount 用户名
     * @return 符合条件的用户集合
     */
    @Override
    public List<User> userSearch(String userAccount, HttpServletRequest request) {
        //判断此用户是否有权限进行此操作
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) attribute;
        if (user == null || user.getUserRole() != ADMIN_ROLE) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(userAccount)) {
            queryWrapper.like("userAccount", userAccount);
        }
        List<User> users = userMapper.selectList(queryWrapper);
        return users.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 通过用户id删除用户
     *
     * @param id 用户id
     * @return 是否成功
     */
    @Override
    public Boolean userDelete(long id, HttpServletRequest request) {
        //判断用户是否有权限
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return this.removeById(id);
    }

    /**
     * 用户脱敏
     *
     * @param originUser 原始用户
     * @return 脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 获取当前用户
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User currentUser = this.getById(user.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return currentUser;
    }

    /**
     * 用户退出
     */
    @Override
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return ResultUtils.success(1);
    }

    /**
     * 通过标签名查询用户
     *
     * @param tagNameList 标签
     * @return 用户列表
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            userQueryWrapper = userQueryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(userQueryWrapper);

        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 修改用户通过用户id
     *
     * @param editUser 要编辑的用户
     * @return 修改后的用户信息
     */
    @Override
    public Boolean updateUser(User editUser, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        Long id = editUser.getId();
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //鉴权
        if (!isAdmin(request) && !currentUser.getId().equals(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = this.getById(editUser.getId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return this.updateById(editUser);
    }

    /**
     * 当前用户是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        return currentUser != null && currentUser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 当前用户是否为管理员
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 首页用户列表
     */
    @Override
    public Page<User> indexUserList(long pageNum, long pageSize, HttpServletRequest request) {
        User currentUser = this.getCurrentUser(request);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //判断缓存是否存在
        String redisKey = String.format("yupao:user:recommend:%s#%s#%s", currentUser.getId(),pageNum,pageSize);
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return userPage;
        }
        //如果没有缓存，查数据库
        userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), new QueryWrapper<>());
        //写入缓存
        try {
            valueOperations.set(redisKey, userPage, 1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return userPage;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("tags", "id");
        userQueryWrapper.isNotNull("tags");
        List<User> userList = this.list(userQueryWrapper);

        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //用户 => 相似度
        ArrayList<Pair<User, Long>> list = new ArrayList<>();
        //依次计算相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离从小到大排序后的id
        List<Long> userIdList = list.stream().sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .map(pair -> pair.getKey().getId())
                .collect(Collectors.toList());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id",userIdList);
        Map<Long, List<User>> map = this.list(queryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        ArrayList<User> finalUserList = new ArrayList<>();
        for (Long id : userIdList) {
            finalUserList.add(map.get(id).get(0));
        }
        return finalUserList;
    }
}


