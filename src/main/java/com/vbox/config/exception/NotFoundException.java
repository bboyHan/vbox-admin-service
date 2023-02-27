package com.vbox.config.exception;

public class NotFoundException extends RuntimeException{

    private static final long serialVersionUID = 0L;
    private String msg;

    public NotFoundException(String msg) {
        super(msg);
    }
}
