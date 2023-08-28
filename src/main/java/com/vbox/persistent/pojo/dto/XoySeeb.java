package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class XoySeeb {

    /**
     * "yp": "银票",
     * "yb": "元宝",
     * "dyp": "大银票",
     * "jgem": "金元宝",
     * "ygem": "银元宝",
     * "kcoin": "金山币",
     * "leftday": "天数",
     * "leftcoin": "通宝",
     * "leftpoint": "点数",
     * "enddate": "到期时间",
     * "leftsecond": "点卡"
     */
    private Integer leftcoin;
    private String enddate;
    private String yp;
    private String yb;
    private String jgem;
    private String ygem;
    private String dyp;
    private String leftday;
    private String leftsecond;

}
