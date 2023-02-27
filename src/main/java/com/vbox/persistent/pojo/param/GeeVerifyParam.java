package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class GeeVerifyParam {

    private String captcha_id;
    private String lot_number;
    private String payload;
    private String process_token;
    private String callback;
    private String w;
}
