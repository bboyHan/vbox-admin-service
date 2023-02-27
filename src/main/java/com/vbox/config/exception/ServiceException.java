package com.vbox.config.exception;

public class ServiceException extends RuntimeException{

    private static final long serialVersionUID = 0L;
    private String msg;

    public ServiceException(String msg) {
        super(msg);
    }
}
