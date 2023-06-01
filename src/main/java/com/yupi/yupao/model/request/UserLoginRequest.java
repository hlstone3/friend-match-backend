package com.yupi.yupao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserLoginRequest implements Serializable {


    private static final long serialVersionUID = 4772243295366861457L;
    private String userAccount;
    private String userPassword;

}
