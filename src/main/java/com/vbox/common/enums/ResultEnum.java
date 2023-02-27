package com.vbox.common.enums;

import java.util.Objects;

public enum ResultEnum {

    ACCESS_LIMIT(500403, "访问过于频繁，超出访问频率限制，请稍后再试"),
    UNAUTHORIZED(500401, "身份验证未通过，请尝试重新授权"),
    NOT_FOUND(500404, "相关资源未找到，请联系管理员"),
    CONFLICT_ERROR(500409, "主Key重复，请重新设置"),
    SERVICE_ERROR(500999, "操作异常，请联系管理员"),
    SUCCESS(200, "成功"),
    ERROR(500999, "系统异常"),
    ;

    private final Integer code;
    private final String msg;

    ResultEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static String of(Integer type) {
        for (ResultEnum enableEnum : ResultEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.msg;
            }
        }
        return ResultEnum.ERROR.getMsg();
    }

    public static Integer valid(Integer type) {
        for (ResultEnum enableEnum : ResultEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.code;
            }
        }
        return ResultEnum.ERROR.getCode();
    }
}
