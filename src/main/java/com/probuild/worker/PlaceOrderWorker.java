package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
public class PlaceOrderWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceOrderWorker.class);
    private static final String API_BASE = "http://localhost:8081";

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "PlaceOrder")
    public void placeOrder(final ActivatedJob job, final JobClient client) {
        LOGGER.info("PlaceOrder fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        String orderId = "order-" + purchaseOrderId;
        LOGGER.info("Placing order with supplier. orderId={}", orderId);

        Integer customerId = null;
        try {
            Map<String, Object> customerBody = new HashMap<>();
            customerBody.put("name", vars.getOrDefault("customerName", "Test Customer"));
            customerBody.put("email", vars.getOrDefault("customerEmail", "test@probuild.local"));
            Map<?, ?> createdCustomer = restTemplate.postForObject(API_BASE + "/customers", customerBody, Map.class);
            if (createdCustomer != null && createdCustomer.get("id") instanceof Integer cid) {
                customerId = cid;
                LOGGER.info("Customer persisted with id={}", customerId);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to persist Customer: {}", e.getMessage());
        }

        Integer toolId = null;
        try {
            String toolUrl = UriComponentsBuilder.fromHttpUrl(API_BASE + "/tools")
                    .queryParam("name", vars.getOrDefault("toolName", "Drill"))
                    .queryParam("category", vars.getOrDefault("toolCategory", "Power Tools"))
                    .toUriString();
            Map<?, ?> createdTool = restTemplate.postForObject(toolUrl, null, Map.class);
            if (createdTool != null && createdTool.get("id") instanceof Integer tid) {
                toolId = tid;
                LOGGER.info("Tool persisted with id={}", toolId);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to persist Tool: {}", e.getMessage());
        }

        Integer apiPurchaseOrderId = null;
        UriComponentsBuilder poUrl = UriComponentsBuilder.fromHttpUrl(API_BASE + "/purchaseorders")
                .queryParam("supplierName", "supplier")
                .queryParam("deliveryManifest", orderId)
                .queryParam("deliveryAddress", vars.getOrDefault("deliveryLocation", "unknown"));
        if (customerId != null) {
            poUrl.queryParam("customerId", customerId);
        }
        try {
            Map<?, ?> created = restTemplate.postForObject(poUrl.toUriString(), null, Map.class);
            LOGGER.info("PurchaseOrder persisted: {}", created);
            if (created != null && created.get("id") instanceof Integer poid) {
                apiPurchaseOrderId = poid;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to persist PurchaseOrder: {}", e.getMessage());
        }

        messagePublisher.publish("Message-order-confirmation", orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        if (toolId != null) {
            result.put("toolId", toolId);
        }
        if (customerId != null) {
            result.put("customerId", customerId);
        }
        if (apiPurchaseOrderId != null) {
            result.put("apiPurchaseOrderId", apiPurchaseOrderId);
        }

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
