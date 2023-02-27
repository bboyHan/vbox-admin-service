package com.vbox.common.enums;

import java.util.Objects;

public enum PayTypeEnum {

    WEI_XIN(1, "weixin"),
    UNKNOWN(-1, "未知"),
    ;

    private final Integer type;
    private final String desc;

    PayTypeEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static String of(Integer type) {
        for (PayTypeEnum genderEnum : PayTypeEnum.values()) {
            if (Objects.equals(genderEnum.type, type)) {
                return genderEnum.desc;
            }
        }
        return UNKNOWN.getDesc();
    }

    public static Integer valid(Integer type) {
        for (PayTypeEnum genderEnum : PayTypeEnum.values()) {
            if (Objects.equals(genderEnum.type, type)) {
                return genderEnum.type;
            }
        }
        return UNKNOWN.getType();
    }
}
