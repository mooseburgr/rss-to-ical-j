package com.mooseburgr.rsstoical.controllers;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ErrorResponseHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ErrorResponseHandler.class);

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> defaultErrorHandler(HttpServletRequest req, Exception e) {
		logger.error("Default exception handler", e);

		return new ResponseEntity<String>(
				"Internal server error:<br><br><pre>" + ExceptionUtils.getStackTrace(e) + "</pre>",
				HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
