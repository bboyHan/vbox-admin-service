package com.vbox.common.enums;

import java.util.Objects;

public enum ChannelEnum {

    JX3_WEIXIN("jx3_weixin", "剑3(微信端)"),
    JX3_WEIXIN_QR("jx3_weixin_qr", "剑3(双端扫码)"),
    JX3_JD("jx3_jd", "剑3(京东端)"),
    JX3_ALIPAY("jx3_alipay", "剑3(支付宝端)"),
    JX3_ALIPAY_PRE("jx3_alipay_pre", "剑3预产(支付宝端)"),
    JX3_ALI_GIFT("jx3_ali_gift", "剑3(支付宝特惠端)"),
    TX_JYM("tx_jym", "QB(支付宝JYM)"),
    TX_ZFB("tx_zfb", "QB(支付宝)"),
    TX_ZFB_2("tx_zfb_2", "QB(支付宝无溢价)"),
    TX_TB("tx_tb", "QB(淘宝)"),
    TX_DY("tx_dy", "QB(抖音)"),
    TX_JD("tx_jd", "QB(京东)"),
    TX_PDD("tx_pdd", "QB(拼多多)"),
    SDO_ALIPAY("sdo_alipay", "盛趣(支付宝)"),
    SDO_IN_ALIPAY("sdo_in_alipay", "SQ(支付宝)"),
    CY_ALIPAY("cy_alipay", "CY(支付宝)"),
    WME_ALIPAY("wme_alipay", "WM(支付宝)"),
    XOY_TB("xoy_tb", "剑三(淘宝)"),
    XOY_JD("xoy_jd", "剑三(京东)"),
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
