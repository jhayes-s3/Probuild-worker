package com.probuild.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * In production these messages would arrive from real external systems
 * (IMS responding to a stock query, supplier sending order confirmation, etc.).
 * For test convenience, throw-side workers self-publish their downstream
 * response messages so a single start trigger walks the whole flow.
 * Remove the publish call when wiring in a real external system.
 */
@Component
public class MessagePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePublisher.class);
    private static final String ZEEBE_PUBLISH_URL = "http://localhost:8080/v2/messages/publication";

    @Autowired
    private RestTemplate restTemplate;

    public void publish(String messageName, String correlationKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", messageName);
        body.put("correlationKey", correlationKey);
        body.put("timeToLive", 30000L);

        LOGGER.info("Auto-publishing {} with correlationKey={}", messageName, correlationKey);
        restTemplate.postForObject(ZEEBE_PUBLISH_URL, body, Map.class);
    }
}
