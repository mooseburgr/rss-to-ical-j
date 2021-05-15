package com.mooseburgr.rsstoical.controllers;

import java.lang.invoke.MethodHandles;
import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mooseburgr.rsstoical.service.ConverterService;

import net.fortuna.ical4j.model.Calendar;

@RestController
public class ICalController {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final ConverterService service;

	public ICalController(ConverterService service) {
		this.service = service;
	}

	@GetMapping("/pub-ical.ics")
	public ResponseEntity<String> getICalConversion(@RequestParam String rssUrl,
			@RequestParam(defaultValue = "60") int eventDuration, HttpServletRequest req) throws SocketException {
		// this should be a serverless function

		logger.info("Handling request: {}{}?{}", req.getServerName(), req.getRequestURI(),
				StringUtils.trimToEmpty(req.getQueryString()));

		Calendar iCal = service.convertRssToICal(rssUrl, eventDuration);

		return ResponseEntity.ok().contentType(new MediaType("text", "calendar")).body(iCal.toString());
	}

}
