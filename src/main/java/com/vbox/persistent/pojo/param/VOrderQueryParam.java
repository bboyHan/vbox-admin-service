package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class VOrderQueryParam {

    private String vouch_code;
    private String captcha_id;
    private String lot_number;
    private String pass_token;
    private String gen_time;
    private String captcha_output;
    private String token;
}
