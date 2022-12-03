package com.mooseburgr.rsstoical.controllers;

import com.mooseburgr.rsstoical.service.ConverterService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.SocketException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ICalController {

  private final ConverterService service;

  @GetMapping("/pub-ical.ics")
  public ResponseEntity<String> getICalConversion(
      @RequestParam String rssUrl,
      @RequestParam(defaultValue = "60") int eventDuration,
      HttpServletRequest req)
      throws SocketException {
    // this should be a serverless function

    log.info(
        "Handling request: {}{}?{}",
        req.getServerName(),
        req.getRequestURI(),
        StringUtils.trimToEmpty(req.getQueryString()));

    Calendar iCal = service.convertRssToICal(rssUrl, eventDuration);

    return ResponseEntity.ok().contentType(new MediaType("text", "calendar")).body(iCal.toString());
  }
}
