package com.vbox.common;

import com.vbox.common.enums.ResultEnum;
import lombok.Data;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Data
public class Result<R> {

    /**
     * data
     */
    private R result;

    /**
     * desc
     */
    private String message;

    /**
     * 0 - success | 1 - error
     */
    private int code;

    public Result() {
    }

    public static <R> ResponseEntity<R> ok() {
        return new ResponseEntity<>(OK);
    }

    public static <R> Result<R> wrap(R data, boolean failed) {
        Result<R> result = new Result<>();
        result.setCode(0);
        result.setMessage(failed ? "ok" : "failed");
        result.setResult(data);
        return result;
    }

    public static <R> Result<R> wrap(R data, ResultEnum resultEnum) {
        Result<R> result = new Result<>();
        result.setCode(resultEnum.getCode());
        result.setMessage(resultEnum.getMsg());
        result.setResult(data);
        return result;
    }

    public static <R> ResponseEntity<Result<R>> ok(R data) {
        Result<R> result = new Result<>();
        result.setCode(1);
        result.setMessage("success");
        result.setResult(data);
        return new ResponseEntity<Result<R>>(result, OK);
    }

    public static <R> ResponseEntity<Result<R>> errBool(R data) {
        Result<R> result = new Result<>();
        result.setCode(0);
        result.setMessage("failed");
        result.setResult(data);
        return new ResponseEntity<Result<R>>(result, INTERNAL_SERVER_ERROR);
    }

    public static <R> ResponseEntity<R> created() {
        return new ResponseEntity<>(CREATED);
    }

    public static <R> ResponseEntity<R> noContent() {
        return new ResponseEntity<>(NO_CONTENT);
    }

    public static <R> ResponseEntity<R> created(R data) {
        return new ResponseEntity<>(data, CREATED);
    }

    public static <R> ResponseEntity<R> error(R data) {
        return new ResponseEntity<>(data, INTERNAL_SERVER_ERROR);
    }

    public static <R> ResponseEntity<R> notFound(R data) {
        return new ResponseEntity<>(data, NOT_FOUND);
    }

    public static <R> ResponseEntity<R> conflict(R data) {
        return new ResponseEntity<>(data, CONFLICT);
    }

    public static <R> ResponseEntity<R> forbidden(R data) {
        return new ResponseEntity<>(data, FORBIDDEN);
    }

    public static <R> ResponseEntity<R> unauthorized(R data) {
        return new ResponseEntity<>(data, UNAUTHORIZED);
    }
}
