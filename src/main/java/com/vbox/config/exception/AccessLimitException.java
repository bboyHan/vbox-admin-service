package com.vbox.config.exception;

import com.vbox.common.enums.ResultEnum;

public class AccessLimitException extends RuntimeException{

    private static final long serialVersionUID = 0L;
    private String msg;

    public AccessLimitException(String msg) {
        super(msg);
    }
}
