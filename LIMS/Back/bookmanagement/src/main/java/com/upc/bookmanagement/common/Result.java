package com.upc.bookmanagement.common;

import lombok.Data;

/**
 * @author han
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-13
 */
@Data
public class Result<T> {
    /**
     * 状态码
     */
    private Integer code;

    /**
     * 状态信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    public Result(Integer code, String message){
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data){
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> fail(Integer code, String message){
        return new Result<>(code, message,  null);
    }
}
