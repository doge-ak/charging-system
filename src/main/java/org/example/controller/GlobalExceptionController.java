package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.util.Result;
import org.example.util.ServiceException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@CrossOrigin(origins = "*")
public class GlobalExceptionController {
    @ExceptionHandler(ServiceException.class)
    public Result<Void> serviceExceptionHandler(ServiceException e) {
        log.error(e.getMessage(), e);
        return new Result<>(e.getCode(), e.getMsg(), null);
    }
}
