package com.pm.userservice.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                409,
                "Resource Conflict",
                ex.getMessage() != null ? ex.getMessage() : "Email already exists"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                404,
                "Resource Not Found",
                ex.getMessage() != null ? ex.getMessage() : "User not found"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}