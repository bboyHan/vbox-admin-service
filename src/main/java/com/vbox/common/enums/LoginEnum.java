package com.vbox.common.enums;

public enum LoginEnum {

    ACCOUNT(1, "账户"),
    WECHAT(2, "微信"),
    QQ(3, "QQ"),
    GITHUB(4, "Github"),
    ;

    private final int type;
    private final String desc;

    LoginEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static LoginEnum of(int type) {
        for (LoginEnum loginEnum : LoginEnum.values()) {
            if (loginEnum.type == type) {
                return loginEnum;
            }
        }
        return ACCOUNT;
    }
}
