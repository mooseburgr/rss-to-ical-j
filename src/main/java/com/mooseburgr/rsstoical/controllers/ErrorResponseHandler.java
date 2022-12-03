package com.mooseburgr.rsstoical.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class ErrorResponseHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> defaultErrorHandler(HttpServletRequest req, Exception e) {
    log.error("Default exception handler", e);

    return new ResponseEntity<String>(
        "Internal server error:<br><br><pre>" + ExceptionUtils.getStackTrace(e) + "</pre>",
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
