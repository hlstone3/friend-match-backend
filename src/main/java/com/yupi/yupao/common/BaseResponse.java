package com.yupi.yupao.common;

import com.yupi.yupao.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {
    //相应状态码
    private int code; //0表示正常，其他表示异常
    //响应数据
    private T data;
    //简略的描述信息
    private String message;
    //详细描述信息
    private String description;

    public BaseResponse(int code, T data, String message) {
        this(code, data, message, "");
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }

    public BaseResponse(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), null, message, errorCode.getDescription());
    }

    public BaseResponse(BusinessException businessException) {
        this(businessException.getCode(), null, businessException.getMessage(), businessException.getDescription());
    }
}
