package com.vbox.common.enums;

import java.util.Objects;

public enum ChannelEnum {

    JX3_WEIXIN("jx3_weixin", "剑3(微信端)"),
    JX3_WEIXIN_QR("jx3_weixin_qr", "剑3(双端扫码)"),
    JX3_JD("jx3_jd", "剑3(京东端)"),
    JX3_ALIPAY("jx3_alipay", "剑3(支付宝端)"),
    JX3_ALIPAY_PRE("jx3_alipay_pre", "剑3预产(支付宝端)"),
    JX3_ALI_GIFT("jx3_ali_gift", "剑3(支付宝特惠端)"),
    TX_JYM("tx_jym", "腾讯(支付宝JYM)"),
    TX_ZFB("tx_zfb", "腾讯(支付宝)"),
    TX_TB("tx_tb", "腾讯(淘宝)"),
    TX_DY("tx_dy", "腾讯(抖音)"),
    TX_JD("tx_jd", "腾讯(京东)"),
    TX_PDD("tx_pdd", "腾讯(拼多多)"),
    SDO_ALIPAY("sdo_alipay", "测试(支付宝)"),
    CY_ALIPAY("cy_alipay", "测试2(支付宝)"),
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
