package com.vbox.common.enums;

import java.util.Objects;

public enum CodeStatusEnum {

    PLATFORM_NOT_PAY(4, "平台查询未支付"),
    TIMEOUT(3, "取码超时"),
    NO_USE(2, "未取码"),
    FINISHED(1, "取码成功"),
    FAILED(0, "取码失败"),
    ;

    private final Integer code;
    private final String msg;

    CodeStatusEnum(int code, String msg) {
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
        for (CodeStatusEnum enableEnum : CodeStatusEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.msg;
            }
        }
        return CodeStatusEnum.FAILED.getMsg();
    }

    public static Integer valid(Integer type) {
        for (CodeStatusEnum enableEnum : CodeStatusEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.code;
            }
        }
        return CodeStatusEnum.FAILED.getCode();
    }
}
