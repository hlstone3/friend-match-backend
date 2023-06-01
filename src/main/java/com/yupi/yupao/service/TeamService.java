package com.yupi.yupao.service;

import com.yupi.yupao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupao.model.domain.User;
import com.yupi.yupao.model.dto.TeamQuery;
import com.yupi.yupao.model.request.TeamJoinRequest;
import com.yupi.yupao.model.request.TeamQuitRequest;
import com.yupi.yupao.model.request.TeamUpdateRequest;
import com.yupi.yupao.model.vo.TeamUserVO;

import java.util.List;

/**
 * @author hongs
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2023-04-07 13:30:15
 */
public interface TeamService extends IService<Team> {

    Long addTeam(Team team, User loginUser);


    List<TeamUserVO> ListTeam(TeamQuery teamQuery, boolean isAdmin);


    Boolean updateTeam(TeamUpdateRequest teamUpdate, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     *
     * @param teamQuitRequest 队伍退出请求体
     * @param loginUser       登录用户
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(long id, User loginUser);

    void hasJoinInit(List<TeamUserVO> teamUserVOList,User currentUser);

}
