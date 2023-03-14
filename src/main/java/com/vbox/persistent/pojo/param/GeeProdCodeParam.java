package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class GeeProdCodeParam {

    private String payload;
    private String token;
    private String encrypt_method;
    private String encrypt_fields;
    private String encrypt_version;
    private String __ts__;
    private String callback;
}
