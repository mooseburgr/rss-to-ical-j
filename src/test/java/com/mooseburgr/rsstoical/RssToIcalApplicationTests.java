package com.mooseburgr.rsstoical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.mooseburgr.rsstoical.controllers.GoogleCloudFunction;
import com.mooseburgr.rsstoical.controllers.ICalController;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.SocketException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import net.fortuna.ical4j.model.Date;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@SpringBootTest
class RssToIcalApplicationTests {

  static final Logger logger = LoggerFactory.getLogger(RssToIcalApplicationTests.class);

  String rssUrl = "https://demo.theeventscalendar.com/events/feed/";
  int eventDuration = 50;

  @Autowired ICalController controller;

  @Test
  void contextLoads() {
    assertTrue(true);
  }

  @Test
  void testRestController() throws SocketException {

    ResponseEntity<String> resp =
        controller.getICalConversion(rssUrl, eventDuration, new MockHttpServletRequest());

    assertEquals(200, resp.getStatusCodeValue());
    assertEquals("text/calendar", resp.getHeaders().getContentType().toString());
    assertTrue(resp.getBody().contains("BEGIN:VCALENDAR"));
    assertTrue(resp.getBody().contains("END:VCALENDAR"));
    assertTrue(resp.getBody().contains("BEGIN:VEVENT"));
    assertTrue(resp.getBody().contains("END:VEVENT"));
  }

  @Test
  void testGoogleCloudFunction() throws IOException {
    HttpRequest req = Mockito.mock(HttpRequest.class);
    when(req.getFirstQueryParameter("rssUrl")).thenReturn(Optional.of(rssUrl));
    when(req.getFirstQueryParameter("eventDuration")).thenReturn(Optional.of("NaN"));
    HttpResponse resp = Mockito.mock(HttpResponse.class);
    StringWriter responseOut = new StringWriter();
    BufferedWriter writerOut = new BufferedWriter(responseOut);
    when(resp.getWriter()).thenReturn(writerOut);

    new GoogleCloudFunction().service(req, resp);

    writerOut.flush();
    String responseBody = responseOut.toString();
    assertTrue(responseBody.contains("BEGIN:VCALENDAR"));
    assertTrue(responseBody.contains("END:VCALENDAR"));
    assertTrue(responseBody.contains("BEGIN:VEVENT"));
    assertTrue(responseBody.contains("END:VEVENT"));
  }

  @Test
  void jwsTest() throws Exception {
    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS512);
    String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    logger.info("encoded public key: {}", publicKey);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
    PublicKey pubKey = kf.generatePublic(keySpecX509);
    assertEquals(keyPair.getPublic(), pubKey);

    String jws =
        Jwts.builder()
            .setIssuer("rss-to-ical-authority")
            .setSubject("user123")
            .setId(UUID.randomUUID().toString())
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
            .setHeaderParam("header-name", "header-value")
            .setHeaderParam("pk", publicKey)
            .claim("authorities", "1,2,3")
            .signWith(keyPair.getPrivate())
            .compact();
    logger.info("JWS: {}", jws);

    Jws<Claims> parsedJws = Jwts.parserBuilder().setSigningKey(pubKey).build().parseClaimsJws(jws);

    assertEquals("header-value", parsedJws.getHeader().get("header-name"));
    assertEquals("user123", parsedJws.getBody().getSubject());
    assertEquals("1,2,3", parsedJws.getBody().get("authorities"));
  }
}
