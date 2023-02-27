package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class SecCode {

    private String captcha_id;
    private String lot_number;
    private String pass_token;
    private String gen_time;
    private String captcha_output;

}
