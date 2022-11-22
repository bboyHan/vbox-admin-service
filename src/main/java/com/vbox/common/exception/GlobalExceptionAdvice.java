package com.vbox.common.exception;

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
}
