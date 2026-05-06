package com.probuild;

import com.probuild.worker.MessagePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessagePublisherTest {

    @Test
    void publishPostsMessageBodyToCamunda() {
        RestTemplate restTemplate = mock(RestTemplate.class);

        MessagePublisher publisher = new MessagePublisher();
        ReflectionTestUtils.setField(publisher, "restTemplate", restTemplate);

        publisher.publish("Message-test", "PO-123", Map.of("approved", true));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(restTemplate).postForObject(
                eq("http://localhost:8080/v2/messages/publication"),
                captor.capture(),
                eq(Map.class)
        );

        assertEquals("Message-test", captor.getValue().get("name"));
        assertEquals("PO-123", captor.getValue().get("correlationKey"));
        assertEquals(30000L, captor.getValue().get("timeToLive"));
        assertEquals(Map.of("approved", true), captor.getValue().get("variables"));
    }
}