package com.vbox.common;

import lombok.Builder;
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
     * type - success | error
     */
    private String type;

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
        result.setType(failed ? "success" : "error");
        result.setMessage(failed ? "ok" : "failed");
        result.setResult(data);
        return result;
    }

    public static <R> ResponseEntity<Result<R>> ok(R data) {
        Result<R> result = new Result<>();
        result.setCode(0);
        result.setType("success");
        result.setMessage("ok");
        result.setResult(data);
        return new ResponseEntity<Result<R>>(result, OK);
    }

    public static <R> ResponseEntity<Result<R>> errBool(R data) {
        Result<R> result = new Result<>();
        result.setCode(-1);
        result.setType("error");
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

    public static <R> ResponseEntity<R> notFound() {
        return new ResponseEntity<>(NOT_FOUND);
    }

}
