package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.exception.BusinessException;
import com.yupi.yupao.mapper.TeamMapper;
import com.yupi.yupao.model.domain.Team;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.domain.UserTeam;
import com.yupi.yupao.model.dto.TeamQuery;
import com.yupi.yupao.model.enums.TeamStatusEnum;
import com.yupi.yupao.model.request.TeamJoinRequest;
import com.yupi.yupao.model.request.TeamQuitRequest;
import com.yupi.yupao.model.request.TeamUpdateRequest;
import com.yupi.yupao.model.vo.TeamUserVO;
import com.yupi.yupao.model.vo.UserVO;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hongs
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-04-07 13:30:15
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    UserTeamService userTeamService;

    @Resource
    UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(Team team, User loginUser) {
        //请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        //3.检验信息
        //(1).队伍人数>1且<=20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);//如果为空，直接赋值为0
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //(2).队伍标题 <=20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        // 3. 描述<= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //4.status 是否公开，不传默认为0
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }

        //5.如果status是加密状态，一定要密码 且密码<=32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum) && (StringUtils.isBlank(password) || password.length() > 32)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
        }
        //6.超出时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超出时间 > 当前时间");
        }

        //7.校验用户最多创建5个队伍
        //todo 有bug。可能同时创建100个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
        }
        //8.插入队伍消息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        //9. 插入用户 ==> 队伍关系 到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        return teamId;
    }

    @Override
    public List<TeamUserVO> ListTeam(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        //从请求参数中取出队伍名称等查询条件，如果存在则作为查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                teamQueryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)){
                teamQueryWrapper.in("id",idList);
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                teamQueryWrapper.like("name", name);
            }
            //通过搜索词对队伍名称和描述进行模糊查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                teamQueryWrapper.like("description", description);
            }
            //查询最大人数相等
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                teamQueryWrapper.eq("maxNum", maxNum);
            }
            //根据创建人来查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                teamQueryWrapper.eq("userId", userId);
            }
            //队伍状态
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            teamQueryWrapper.eq("status", statusEnum.getValue());
        }
        //不展示已过期的队伍
        //expireTime is null or expireTime > now()
        teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> list = this.list(teamQueryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        //关联查询创建人的用户信息
        for (Team team : list) {
            Long userId = team.getUserId();
            if (userId == null || userId < 0L) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //脱敏
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public Boolean updateTeam(TeamUpdateRequest teamUpdate, User loginUser) {
        //判断请求参数是否为空
        if (teamUpdate == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询队伍是否存在
        Long id = teamUpdate.getId();
        if (id == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        //只有管理员或者队伍的创建者才可以修改
        if (!userService.isAdmin(loginUser) && !team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //如果从公开房间改为加密房间需要设置密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdate.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET) && (StringUtils.isBlank(teamUpdate.getPassword()))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间需要设置密码");
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdate, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
         /* 其他人、未满、未过期，允许加入多个队伍，但是要有个上限 P0
            1. 用户最多加入 5 个队伍
            2. 队伍必须存在，只能加入未满、未过期的队伍
            3. 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
            4. 禁止加入私有的队伍
            5. 如果加入的队伍是加密的，必须密码匹配才可以
            6. 新增队伍 - 用户关联信息
        */

        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);

        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        Long userId = loginUser.getId();
        //2. 队伍必须存在，只能加入未满、未过期的队伍
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍过期");
        }
        userTeamQueryWrapper.eq("teamId", teamId);
        long teamHasNum = userTeamService.count(userTeamQueryWrapper);
        if (teamHasNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }
        //3. 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count >= 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        //4. 禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if (enumByValue.equals(TeamStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私密队伍");
        }
        //5. 如果加入的队伍是加密的，必须密码匹配才可以
        String password = teamJoinRequest.getPassword();
        if (enumByValue.equals(TeamStatusEnum.SECRET) && (StringUtils.isBlank(password) || !password.equals(team.getPassword()))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");

        }
        //1. 用户最多加入 5 个队伍
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        long joinTeamNum = userTeamService.count(userTeamQueryWrapper);
        if (joinTeamNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多可加入5个队伍");
        }
        //6. 修改用户关联信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }

    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
/*      1. 校验请求参数
        2. 校验队伍是否存在
        3. 校验我是否已加入队伍
        4. 如果队伍
            1. 只剩一人，队伍解散
            2. 还有其他人
                1. 如果是队长退出队伍，权限转移给第二早加入的用户 —— 先来后到只用取 id 最小的 2 条数据
                2. 非队长，自己退出队伍*/

        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = this.getTeamById(teamId);
        //3. 校验我是否已加入队伍
        Long userId = loginUser.getId();
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户没有加入队伍");
        }
        //4
        long teamHasJoinNum = countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) {
            //只剩一人，队伍解散
            this.removeById(teamId);
        } else {
            if (userId.equals(team.getUserId())) {
                //是队长
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextTeam.getUserId();
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队长失败");
                }
            }
        }

        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        Team team = this.getTeamById(id);
        //鉴权
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", id);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除用户队伍关联信息失败");
        }
        //删除队伍
        return this.removeById(id);
    }


    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

    /**
     * 根据队伍Id获取队伍
     *
     * @param teamId 队伍Id
     * @return 队伍
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 判断当前用户是否加入该队伍,初始化hasJoin
     */
    public void hasJoinInit(List<TeamUserVO> teamUserVOList, User currentUser){

        //判断是否加入队伍
        List<Long> teamListId = teamUserVOList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",currentUser.getId());
        userTeamQueryWrapper.in("teamId",teamListId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        Set<Long> hasJoinTeamSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        teamUserVOList.forEach(team ->{
            boolean hasJoin = hasJoinTeamSet.contains(team.getId());
            team.setHasJoin(hasJoin);
        });
    }
}




