package com.mooseburgr.rsstoical.controllers;

import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.net.URI;
import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import io.jsonwebtoken.io.IOException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.FixedUidGenerator;
import net.fortuna.ical4j.util.SimpleHostInfo;
import net.fortuna.ical4j.util.UidGenerator;

@RestController
public class ICalController {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final RestTemplate client = new RestTemplate(new OkHttp3ClientHttpRequestFactory());

	@GetMapping("/pub-ical.ics")
	public ResponseEntity<String> getICalConversion(@RequestParam String rssUrl,
			@RequestParam(defaultValue = "60") int eventDuration, HttpServletRequest req) throws SocketException {
		// this should be a serverless function
		
		logger.info("Handling request: {}{}?{}", req.getServerName(), req.getRequestURI(),
				StringUtils.trimToEmpty(req.getQueryString()));

		if(logger.isDebugEnabled()) {
			logger.debug("RSS response: {}", client.getForEntity(rssUrl, String.class));
		}		

		SyndFeed feed = client.execute(rssUrl, HttpMethod.GET, null, response -> {
			SyndFeedInput input = new SyndFeedInput();
			try {
				return input.build(new XmlReader(response.getBody()));
			} catch (Exception e) {
				logger.error("Failed to parse RSS feed", e);
				throw new IOException("Could not parse response", e);
			}
		});
		logger.debug("parsed SyndFeed: {}", feed);

		Calendar ical = new Calendar();
		String prodId = "-//" + feed.getTitle() + "//mooseburgr/rss-to-ical";
		ical.getProperties().add(new ProdId(prodId));
		ical.getProperties().add(Version.VERSION_2_0);
		ical.getProperties().add(CalScale.GREGORIAN);
		ical.getProperties().add(Method.PUBLISH);
		ical.getProperties().add(new XProperty("X-WR-CALNAME", feed.getTitle()));
		ical.getProperties().add(new XProperty("X-WR-CALDESC", feed.getDescription()));
		ical.getProperties().add(new Url(URI.create(feed.getLink())));
		ical.getProperties().add(new XProperty("X-WR-RELCALID", DigestUtils.sha256Hex(prodId)));

		UidGenerator uidGenerator = new FixedUidGenerator(new SimpleHostInfo(URI.create(rssUrl).getHost()),
				feed.getTitle());

		feed.getEntries().stream().forEach(entry -> {
			VEvent event = new VEvent(new DateTime(entry.getPublishedDate()),
					new DateTime(Date.from(entry.getPublishedDate().toInstant().plus(Duration.ofMinutes(eventDuration)))),
					entry.getTitle());
			event.getProperties().add(uidGenerator.generateUid());
			event.getProperties().add(new Description(entry.getDescription().getValue()));
			event.getProperties().add(new Location(entry.getLink()));
			event.getProperties().add(new Summary(entry.getTitle()));
			event.getProperties().add(new Url(URI.create(entry.getUri())));

			ical.getComponents().add(event);
		});
		logger.info("generated calendar: {}", ical);

		return ResponseEntity.ok().contentType(new MediaType("text", "calendar")).body(ical.toString());
	}

}
