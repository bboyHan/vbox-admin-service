package com.vbox.common.enums;

public enum GenderEnum {

    MAN(0, "男"),
    FEMALE(1, "女"),
    ;

    private final int type;
    private final String desc;

    GenderEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static String of(int type) {
        for (GenderEnum genderEnum : GenderEnum.values()) {
            if (genderEnum.type == type) {
                return genderEnum.desc;
            }
        }
        return null;
    }
}
