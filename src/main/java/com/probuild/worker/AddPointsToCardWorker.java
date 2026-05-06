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
public class AddPointsToCardWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddPointsToCardWorker.class);
    private static final String API_BASE = "http://localhost:8081";
    /**
     * Trade Card: members earn points at 5% of the order total. Points later
     * roll up into a discount band, capped at 10% off. Points reset annually.
     */
    private static final double POINTS_RATE = 0.05;

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-add-points-to-card")
    public void addPointsToCard(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-add-points-to-card fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        Object customerId = vars.get("customerId");

        if (customerId instanceof Integer cid) {
            String tradeCardNumber = (String) vars.getOrDefault(
                    "tradeCardNumber", "TC-" + purchaseOrderId);

            int pointsToAdd = calculatePoints(vars.get("finalPrice"));

            Integer cardId = findOrCreateTradeCard(tradeCardNumber, cid);
            if (cardId != null && pointsToAdd > 0) {
                String addUrl = UriComponentsBuilder.fromHttpUrl(
                                API_BASE + "/tradecards/" + cardId + "/points")
                        .queryParam("pointsToAdd", pointsToAdd)
                        .toUriString();
                try {
                    Map<?, ?> updated = restTemplate.postForObject(addUrl, null, Map.class);
                    LOGGER.info("Added {} points to TradeCard {}; new balance: {}",
                            pointsToAdd, cardId,
                            updated == null ? "?" : updated.get("pointsBalance"));
                } catch (Exception e) {
                    LOGGER.warn("Failed to add points: {}", e.getMessage());
                }

                Object apiPoId = vars.get("apiPurchaseOrderId");
                if (apiPoId instanceof Integer poid) {
                    String linkUrl = UriComponentsBuilder.fromHttpUrl(
                                    API_BASE + "/purchaseorders/" + poid)
                            .queryParam("tradeCardId", cardId)
                            .toUriString();
                    try {
                        restTemplate.put(linkUrl, null);
                        LOGGER.info("Linked TradeCard {} to PurchaseOrder {}", cardId, poid);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to link TradeCard to PurchaseOrder: {}", e.getMessage());
                    }
                }
            }
        } else {
            LOGGER.info("No customerId in variables for PO={}; skipping trade card points", purchaseOrderId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pointsAdded", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    /** Trade Card points: 5% of the order's final price, rounded down. */
    private int calculatePoints(Object finalPriceVar) {
        if (finalPriceVar == null) return 0;
        try {
            double finalPrice = Double.parseDouble(finalPriceVar.toString());
            return (int) Math.floor(finalPrice * POINTS_RATE);
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse finalPrice='{}' to a number; awarding 0 points", finalPriceVar);
            return 0;
        }
    }

    /** Look up an existing TradeCard by number; if missing, create one for the customer. */
    private Integer findOrCreateTradeCard(String cardNumber, Integer customerId) {
        try {
            Map<?, ?> existing = restTemplate.getForObject(
                    API_BASE + "/tradecards/number/" + cardNumber, Map.class);
            if (existing != null && existing.get("id") instanceof Integer id) {
                return id;
            }
        } catch (Exception e) {
            // 404 / parse fallthrough — fall to create
        }
        try {
            String createUrl = UriComponentsBuilder.fromHttpUrl(API_BASE + "/tradecards")
                    .queryParam("cardNumber", cardNumber)
                    .queryParam("customerId", customerId)
                    .toUriString();
            Map<?, ?> created = restTemplate.postForObject(createUrl, null, Map.class);
            if (created != null && created.get("id") instanceof Integer id) {
                LOGGER.info("Created TradeCard {} for Customer {}", cardNumber, customerId);
                return id;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create TradeCard: {}", e.getMessage());
        }
        return null;
    }
}
