package com.probuild.worker.customer;

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
import java.util.UUID;

@Component
public class CardPaymentWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CardPaymentWorker.class);

    private static final String API_BASE = "http://localhost:8081";
    /**
     * Trade Card discount band — three tiers, capped at 10%.
     * Anyone with a card gets at least 2% (entry tier — no 0% so the £10/year
     * membership is worth signing up for).
     *   0-499 pts     = 2%   (entry)
     *   500-1999 pts  = 5%   (mid)
     *   2000+ pts     = 10%  (top)
     */
    private static final double DISCOUNT_TIER_ENTRY = 0.02;
    private static final double DISCOUNT_TIER_MID = 0.05;
    private static final double DISCOUNT_TIER_TOP = 0.10;
    private static final int TIER_MID_THRESHOLD = 500;
    private static final int TIER_TOP_THRESHOLD = 2000;

    @Autowired
    private RestTemplate restTemplate;

    private static final Map<String, Double> TOOL_PRICES = Map.of(
            "drill", 15.00,
            "saw", 20.00,
            "sander", 12.50,
            "ladder", 10.00,
            "cement mixer", 35.00,
            "pressure washer", 25.00
    );

    @JobWorker(type = "job-card-payment")
    public void cardPayment(final ActivatedJob job, final JobClient client) {

        LOGGER.info("Card payment worker started. Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> variables = job.getVariablesAsMap();

        String selectedToolName = getString(variables, "selectedToolName");
        String requestedToolName = getString(variables, "toolName");

        String toolNameToUse;
        if (!selectedToolName.isBlank() && !selectedToolName.equalsIgnoreCase("none")) {
            toolNameToUse = selectedToolName;
        } else {
            toolNameToUse = requestedToolName;
        }

        int quantity = getIntOrDefault(variables, "quantity", 1);
        int duration = getIntOrDefault(variables, "duration", 1);
        boolean membershipBeingUsed = getBooleanOrDefault(variables, "membershipBeingUsed", false);

        double dailyToolPrice = getToolPrice(toolNameToUse);
        double originalPrice = dailyToolPrice * quantity * duration;

        double discountRate = 0.00;
        Integer pointsBalance = null;
        if (membershipBeingUsed) {
            pointsBalance = lookupTradeCardPoints(variables.get("customerId"));
            discountRate = pointsToDiscountRate(pointsBalance);
            LOGGER.info("Trade card points={} -> discount band {}%",
                    pointsBalance, (int) (discountRate * 100));
        }
        double discountAmount = originalPrice * discountRate;
        double finalPrice = originalPrice - discountAmount;
        boolean moreThan100 = finalPrice > 100.00;

        String cardName = getString(variables, "cardName");
        String cardNumber = cleanCardNumber(getString(variables, "cardNumber"));
        String cardExpiry = getString(variables, "cardExpiry");
        String cardCvv = getString(variables, "cardCvv");

        boolean paymentAccepted =
                !cardName.isBlank()
                        && isValidCardNumber(cardNumber)
                        && cardExpiry.matches("^(0[1-9]|1[0-2])/\\d{2}$")
                        && cardCvv.matches("\\d{3,4}");

        String paymentReference = "PAY-" + UUID.randomUUID();

        Map<String, Object> result = new HashMap<>();
        result.put("paymentProcessed", true);
        result.put("paymentAccepted", paymentAccepted);
        result.put("paymentReference", paymentReference);

        result.put("toolName", toolNameToUse);
        result.put("quantity", quantity);
        result.put("duration", duration);
        result.put("dailyToolPrice", dailyToolPrice);
        result.put("originalPrice", roundToMoney(originalPrice));
        result.put("membershipDiscountApplied", membershipBeingUsed);
        result.put("discountRate", discountRate);
        result.put("discountAmount", roundToMoney(discountAmount));
        result.put("finalPrice", roundToMoney(finalPrice));
        result.put("moreThan100", moreThan100);
        if (pointsBalance != null) {
            result.put("tradeCardPointsAtCheckout", pointsBalance);
        }

        result.put("maskedCardNumber", maskCardNumber(cardNumber));

        if (paymentAccepted) {
            result.put("paymentMessage", "Payment accepted. Total paid: £" + roundToMoney(finalPrice));
        } else {
            result.put("paymentMessage", "Payment declined. Please check the card details and try again.");
        }

        LOGGER.info("Payment accepted: {}", paymentAccepted);
        LOGGER.info("Final price calculated: £{}", roundToMoney(finalPrice));

        if (paymentAccepted) {
            persistCardDetailsToCustomer(variables, cardNumber, cardExpiry, cardCvv);
        }

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    private void persistCardDetailsToCustomer(Map<String, Object> variables,
                                              String cardNumber, String cardExpiry, String cardCvv) {
        Object customerId = variables.get("customerId");
        if (!(customerId instanceof Integer cid)) {
            LOGGER.info("No customerId in variables; skipping customer card update");
            return;
        }
        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/customers/" + cid);
        if (!cardNumber.isBlank()) url.queryParam("cardNumber", maskCardNumber(cardNumber));
        if (!cardExpiry.isBlank()) url.queryParam("cardExpiry", cardExpiry);
        if (!cardCvv.isBlank()) url.queryParam("cardCvc", cardCvv);
        try {
            restTemplate.put(url.toUriString(), null);
            LOGGER.info("Customer {} updated with payment card details", cid);
        } catch (Exception e) {
            LOGGER.warn("Failed to update Customer card details: {}", e.getMessage());
        }
    }

    /** Convert a points balance to a discount rate. Card-holders always get
     *  at least the entry tier; reaching higher tiers bumps it. */
    private double pointsToDiscountRate(Integer pointsBalance) {
        int pts = pointsBalance == null ? 0 : pointsBalance;
        if (pts >= TIER_TOP_THRESHOLD) return DISCOUNT_TIER_TOP;
        if (pts >= TIER_MID_THRESHOLD) return DISCOUNT_TIER_MID;
        return DISCOUNT_TIER_ENTRY;
    }

    /** Look up the customer's trade card and return the highest points balance, or null if no card. */
    @SuppressWarnings("unchecked")
    private Integer lookupTradeCardPoints(Object customerIdVar) {
        if (!(customerIdVar instanceof Integer cid)) return null;
        try {
            Object[] cards = restTemplate.getForObject(
                    API_BASE + "/tradecards/customer/" + cid, Object[].class);
            if (cards == null || cards.length == 0) return null;
            int best = 0;
            for (Object o : cards) {
                if (o instanceof Map<?, ?> m && m.get("pointsBalance") instanceof Integer p) {
                    best = Math.max(best, p);
                }
            }
            return best;
        } catch (Exception e) {
            LOGGER.warn("Failed to look up trade card points for Customer {}: {}", cid, e.getMessage());
            return null;
        }
    }

    private double getToolPrice(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return 10.00;
        }

        return TOOL_PRICES.getOrDefault(toolName.toLowerCase(), 10.00);
    }

    private String getString(Map<String, Object> variables, String key) {
        Object value = variables.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }

    private int getIntOrDefault(Map<String, Object> variables, String key, int defaultValue) {
        Object value = variables.get(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanOrDefault(Map<String, Object> variables, String key, boolean defaultValue) {
        Object value = variables.get(key);

        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.toString());
    }

    private String cleanCardNumber(String cardNumber) {
        return cardNumber.replaceAll("[^0-9]", "");
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFourDigits;
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{13,19}")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    private double roundToMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}