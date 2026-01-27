package com.example.exception;

import com.example.commons.RestAPIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestAPIResponse> handleBusinessException(BusinessException ex) {
        return new ResponseEntity<>(
            new RestAPIResponse("failed", ex.getMessage(), null),
            HttpStatus.OK   // 👈 returns 200 instead of 403
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestAPIResponse> handleRuntimeException(RuntimeException ex) {
        return new ResponseEntity<>(
            new RestAPIResponse("failed", ex.getMessage(), null),
            HttpStatus.OK
        );
    }
}
