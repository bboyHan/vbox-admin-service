package com.vbox.config.exception;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.io.IORuntimeException;
import com.vbox.common.Result;
import com.vbox.common.enums.ResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {

//    @ExceptionHandler(value = Exception.class)
//    @ResponseBody
//    public ResponseEntity<Result<String>> handleEx(HttpServletRequest req, Exception e) {
//        log.error(req.getRequestURI(), e);
//        return Result.error(Result.wrap(e.getMessage(), ResultEnum.ERROR));
//    }

    @ExceptionHandler(value = ValidateException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleValidateEx(HttpServletRequest req, ValidateException e) {
        log.error(req.getRequestURI(), e);
        return Result.unauthorized(Result.wrap(e.getMessage(), ResultEnum.UNAUTHORIZED));
    }

    @ExceptionHandler(value = AccessLimitException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleAccessLimitEx(HttpServletRequest req, AccessLimitException e) {
        log.error(req.getRequestURI(), e);
        return Result.forbidden(Result.wrap(e.getMessage(), ResultEnum.ACCESS_LIMIT));
    }

    @ExceptionHandler(value = NotFoundException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleNotFoundEx(HttpServletRequest req, NotFoundException e) {
        log.error(req.getRequestURI(), e);
        return Result.notFound(Result.wrap(e.getMessage(), ResultEnum.NOT_FOUND));
    }

    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleServiceEx(HttpServletRequest req, NotFoundException e) {
        log.error(req.getRequestURI(), e);
        return Result.notFound(Result.wrap(e.getMessage(), ResultEnum.SERVICE_ERROR));
    }

    @ExceptionHandler(value = IORuntimeException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleIORuntimeEx(HttpServletRequest req, IORuntimeException e) {
        log.error(req.getRequestURI(), e);
        return Result.notFound(Result.wrap(e.getMessage(), ResultEnum.SERVICE_ERROR));
    }

    @ExceptionHandler(value = DuplicateKeyException.class)
    @ResponseBody
    public ResponseEntity<Result<String>> handleDuplicateKeyEx(HttpServletRequest req, DuplicateKeyException e) {
        log.error(req.getRequestURI(), e);
        return Result.conflict(Result.wrap(e.getMessage(), ResultEnum.CONFLICT_ERROR));
    }
}
