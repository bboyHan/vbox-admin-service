//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.vbox.common.enums;

import java.util.Objects;

public enum CodeUseStatusEnum {
    PLATFORM_NOT_PAY(4, "平台查询未支付"),
    TIMEOUT(3, "取码超时"),
    NO_USE(2, "未取码"),
    FINISHED(1, "取码成功"),
    FAILED(0, "取码失败");

    private final Integer code;
    private final String msg;

    private CodeUseStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

    public static String of(Integer type) {
        CodeUseStatusEnum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            CodeUseStatusEnum enableEnum = var1[var3];
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.msg;
            }
        }

        return FAILED.getMsg();
    }

    public static Integer valid(Integer type) {
        CodeUseStatusEnum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            CodeUseStatusEnum enableEnum = var1[var3];
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.code;
            }
        }

        return FAILED.getCode();
    }
}
