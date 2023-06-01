package com.yupi.yupao.model.dto;

import com.yupi.yupao.common.PageRequest;
import lombok.Data;

import java.util.List;

@Data
public class TeamQuery extends PageRequest {
    private static final long serialVersionUID = -1413669802736848077L;
    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 我加入的队伍id列表
     */
    private List<Long> idList;
}