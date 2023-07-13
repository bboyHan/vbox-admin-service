package com.vbox.common.enums;

import java.util.Objects;

public enum OrderStatusEnum {

    PAY_TIMEOUT(3, "支付超时"),
    NO_PAY(2, "未支付"),
    PAY_CREATING(4, "待进单"),
    PAY_CREATING_ERROR(5, "订单异常"),
    PAY_FINISHED(1, "已支付"),
    PAY_FAILED(0, "支付失败"),
    ;

    private final Integer code;
    private final String msg;

    OrderStatusEnum(int code, String msg) {
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
        for (OrderStatusEnum enableEnum : OrderStatusEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.msg;
            }
        }
        return OrderStatusEnum.PAY_FAILED.getMsg();
    }

    public static Integer valid(Integer type) {
        for (OrderStatusEnum enableEnum : OrderStatusEnum.values()) {
            if (Objects.equals(enableEnum.code, type)) {
                return enableEnum.code;
            }
        }
        return OrderStatusEnum.PAY_FAILED.getCode();
    }
}
