package com.example.taskhub.exception;

import org.springframework.http.HttpStatus;

/** One exception type carrying an HTTP status. GlobalExceptionHandler turns it into RFC 9457 problem details. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String msg)     { return new ApiException(HttpStatus.NOT_FOUND, msg); }
    public static ApiException forbidden(String msg)    { return new ApiException(HttpStatus.FORBIDDEN, msg); }
    public static ApiException unauthorized(String msg) { return new ApiException(HttpStatus.UNAUTHORIZED, msg); }
    public static ApiException conflict(String msg)     { return new ApiException(HttpStatus.CONFLICT, msg); }
    public static ApiException badRequest(String msg)   { return new ApiException(HttpStatus.BAD_REQUEST, msg); }
    public static ApiException unprocessable(String msg){ return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, msg); }
}
