package com.vbox.common.enums;

import java.util.Objects;

public enum JXGatewayEnum {

    z01("z01", "电信一区（点月卡）"),
    z05("z05", "电信五区（点卡）"),
    z08("z08", "电信八区（点卡）"),
    z21("z21", "双线一区（点卡）"),
    z22("z22", "双线二区（点月卡）"),
    z24("z24", "双线四区（点卡）"),
    ;

    private final String type;
    private final String desc;

    JXGatewayEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static String of(String type) {
        for (JXGatewayEnum ele : JXGatewayEnum.values()) {
            if (Objects.equals(ele.type, type)) {
                return ele.getDesc();
            }
        }
        return null;
    }
}
