package com.vbox.common.enums;

import java.util.Objects;

public enum GenderEnum {

    MAN(0, "男"),
    FEMALE(1, "女"),
    UNKNOWN(-1, "未知"),
    ;

    private final Integer type;
    private final String desc;

    GenderEnum(int type, String desc) {
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
        for (GenderEnum genderEnum : GenderEnum.values()) {
            if (Objects.equals(genderEnum.type, type)) {
                return genderEnum.desc;
            }
        }
        return UNKNOWN.getDesc();
    }

    public static Integer valid(Integer type) {
        for (GenderEnum genderEnum : GenderEnum.values()) {
            if (Objects.equals(genderEnum.type, type)) {
                return genderEnum.type;
            }
        }
        return UNKNOWN.getType();
    }
}
