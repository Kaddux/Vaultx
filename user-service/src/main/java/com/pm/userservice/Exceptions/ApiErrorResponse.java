package com.pm.userservice.Exceptions;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class ApiErrorResponse {
    private Integer status;
    private String error;
    private String message;
    private Map<String,String> fieldErrors;
    private LocalDateTime timestamp;

    public ApiErrorResponse(Integer status, String error,
                            String message, Map<String,String > fieldErrors, LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }
    public ApiErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
