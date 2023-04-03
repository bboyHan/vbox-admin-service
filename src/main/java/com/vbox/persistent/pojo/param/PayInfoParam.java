package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class PayInfoParam {

    private String passport;
    private String gateway;
    private String ck;
    private Integer money;

}
