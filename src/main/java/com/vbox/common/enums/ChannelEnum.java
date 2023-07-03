package com.vbox.common.enums;

import java.util.Objects;

public enum ChannelEnum {

    JX3_WEIXIN("jx3_weixin", "剑网3(微信端)"),
    JX3_WEIXIN_QR("jx3_weixin_qr", "剑网3(双端扫码)"),
    JX3_JD("jx3_jd", "剑网3(京东端)"),
    JX3_ALIPAY("jx3_alipay", "剑网3(支付宝端)"),
    JX3_ALI_GIFT("jx3_ali_gift", "剑网3(支付宝特惠端)"),
    UNKNOWN("unknown", "未知"),
    ;

    private final String code;
    private final String msg;

    ChannelEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static String of(String code) {
        for (ChannelEnum enableEnum : ChannelEnum.values()) {
            if (Objects.equals(enableEnum.code, code)) {
                return enableEnum.msg;
            }
        }
        return ChannelEnum.UNKNOWN.getMsg();
    }

    public static String valid(String code) {
        for (ChannelEnum enableEnum : ChannelEnum.values()) {
            if (Objects.equals(enableEnum.code, code)) {
                return enableEnum.code;
            }
        }
        return ChannelEnum.UNKNOWN.getCode();
    }
}
