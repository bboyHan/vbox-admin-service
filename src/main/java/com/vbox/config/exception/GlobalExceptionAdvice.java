package com.vbox.config.exception;

import cn.hutool.core.exceptions.ValidateException;
import com.vbox.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleEx(HttpServletRequest req, Exception e) {
        log.error(req.getRequestURI(), e);
        return Result.error(Result.wrap(e.getMessage(), false));
    }

    @ExceptionHandler(value = ValidateException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleValidateEx(HttpServletRequest req, ValidateException e) {
        log.error(req.getRequestURI(), e);
        return Result.unauthorized(Result.wrap(e.getMessage(), false));
    }
}
