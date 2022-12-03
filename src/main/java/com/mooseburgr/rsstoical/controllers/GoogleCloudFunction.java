package com.mooseburgr.rsstoical.controllers;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.mooseburgr.rsstoical.service.ConverterService;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

public class GoogleCloudFunction implements HttpFunction {

  private static final ConverterService service = new ConverterService();
  private static final String DEFAULT_EVENT_DURATION = "60";

  @Override
  public void service(HttpRequest request, HttpResponse response) throws IOException {
    var rssUrl = request.getFirstQueryParameter("rssUrl");
    if (rssUrl.isEmpty()) {
      response.setStatusCode(HttpStatus.BAD_REQUEST.value(), "Parameter 'rssUrl' is required");
      return;
    }
    var eventDuration = request.getFirstQueryParameter("eventDuration");
    if (eventDuration.isPresent() && !StringUtils.isNumeric(eventDuration.get())) {
      eventDuration = Optional.of(DEFAULT_EVENT_DURATION);
    }

    var iCal =
        service.convertRssToICal(
            rssUrl.get(), Integer.parseInt(eventDuration.orElse(DEFAULT_EVENT_DURATION)));

    response.setStatusCode(HttpStatus.OK.value());
    response.setContentType("text/calendar");
    response.getWriter().write(iCal.toString());
  }
}
