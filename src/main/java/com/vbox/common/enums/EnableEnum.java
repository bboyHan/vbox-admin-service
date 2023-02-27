package com.vbox.common.enums;

import java.util.Objects;

public enum EnableEnum {

    DISABLE(0, "禁用"),
    ENABLE(1, "开启"),
    ;

    private final Integer type;
    private final String desc;

    EnableEnum(int type, String desc) {
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
        for (EnableEnum enableEnum : EnableEnum.values()) {
            if (Objects.equals(enableEnum.type, type)) {
                return enableEnum.desc;
            }
        }
        return EnableEnum.DISABLE.getDesc();
    }

    public static Integer valid(Integer type) {
        for (EnableEnum enableEnum : EnableEnum.values()) {
            if (Objects.equals(enableEnum.type, type)) {
                return enableEnum.type;
            }
        }
        return EnableEnum.DISABLE.getType();
    }
}
