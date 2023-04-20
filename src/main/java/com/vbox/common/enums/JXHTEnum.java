package com.vbox.common.enums;

import java.util.Objects;

public enum JXHTEnum {

    //76，156，162，276
    _76(1, 76),
    _156(2, 156),
    _126(3, 126),
    _276(4, 276),
    UNKNOWN(-1, -1),
    ;

    private final Integer type;
    private final Integer money;

    JXHTEnum(int type, int money) {
        this.type = type;
        this.money = money;
    }

    public Integer getType() {
        return type;
    }

    public Integer getMoney() {
        return money;
    }

    public static Integer of(Integer type) {
        for (JXHTEnum genderEnum : JXHTEnum.values()) {
            if (Objects.equals(genderEnum.type, type)) {
                return genderEnum.money;
            }
        }
        return UNKNOWN.getMoney();
    }

    public static Integer type(Integer money) {
        for (JXHTEnum genderEnum : JXHTEnum.values()) {
            if (Objects.equals(genderEnum.money, money)) {
                return genderEnum.type;
            }
        }
        return UNKNOWN.getType();
    }
}
