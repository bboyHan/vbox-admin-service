package com.vbox.common.enums;

import java.util.Objects;

public enum OrderCallbackEnum {

    NOT_CALLBACK(2, "未回调"),
    ALREADY_CALLBACK(1, "已回调"),
    CALLBACK_FAILED(0, "回调失败"),
    ;

    private final Integer code;
    private final String msg;

    OrderCallbackEnum(int code, String msg) {
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
        for (OrderCallbackEnum enableEnum : OrderCallbackEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.msg;
            }
        }
        return OrderCallbackEnum.CALLBACK_FAILED.getMsg();
    }

    public static Integer valid(Integer type) {
        for (OrderCallbackEnum enableEnum : OrderCallbackEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.code;
            }
        }
        return OrderCallbackEnum.CALLBACK_FAILED.getCode();
    }
}
