package com.vbox.config.exception;

public class UnSupportException extends RuntimeException{

    private static final long serialVersionUID = 0L;
    private String msg;

    public UnSupportException(String msg) {
        super(msg);
    }
}
