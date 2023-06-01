package com.yupi.yupao.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserDeleteRequest implements Serializable {
    private static final long serialVersionUID = 5586412082866429350L;

    long id;
}
