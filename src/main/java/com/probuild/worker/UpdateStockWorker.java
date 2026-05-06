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
public class UpdateStockWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStockWorker.class);
    private static final String API_BASE = "http://localhost:8081";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-update-stock")
    public void updateStock(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-update-stock fired, Job ID: {}", job.getKey());
        Map<String, Object> vars = job.getVariablesAsMap();
        Object purchaseOrderId = vars.get("purchaseOrderId");
        Object toolId = vars.get("toolId");

        Object quantity = firstNonNull(
                vars.get("quantityMoved"),
                vars.get("quantityAccepted"),
                1);

        String binLocation = resolveBinLocation(vars);

        if (toolId instanceof Integer tid) {
            String url = UriComponentsBuilder.fromHttpUrl(API_BASE + "/stock")
                    .queryParam("toolId", tid)
                    .queryParam("quantity", quantity)
                    .queryParam("binLocation", binLocation)
                    .toUriString();
            try {
                Map<?, ?> created = restTemplate.postForObject(url, null, Map.class);
                LOGGER.info("StockRecord persisted: {}", created);
            } catch (Exception e) {
                LOGGER.warn("Failed to persist StockRecord: {}", e.getMessage());
            }
        } else {
            LOGGER.warn("No toolId in variables for PO {}; skipping stock record creation", purchaseOrderId);
        }

        Object apiPoId = vars.get("apiPurchaseOrderId");
        Object verifiedSupplierName = vars.get("supplierName");
        Object docRef = vars.get("documentReferenceNumber");
        if (apiPoId instanceof Integer poid && (verifiedSupplierName != null || docRef != null)) {
            UriComponentsBuilder poUpdate = UriComponentsBuilder.fromHttpUrl(API_BASE + "/purchaseorders/" + poid);
            if (verifiedSupplierName != null) {
                poUpdate.queryParam("supplierName", verifiedSupplierName.toString());
            }
            if (docRef != null) {
                poUpdate.queryParam("deliveryManifest", docRef.toString());
            }
            try {
                restTemplate.put(poUpdate.toUriString(), null);
                LOGGER.info("PurchaseOrder {} updated with verified supplier data", poid);
            } catch (Exception e) {
                LOGGER.warn("Failed to update PurchaseOrder: {}", e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stockUpdated", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    /** Pick the most authoritative bin reference available. */
    private String resolveBinLocation(Map<String, Object> vars) {
        Object destination = vars.get("destinationLocation");
        if (destination instanceof String s && !s.isBlank()) {
            return s;
        }
        Object suggested = vars.get("suggestedBin");
        if (suggested instanceof String s && !s.isBlank()) {
            return s;
        }
        Object aisle = vars.get("assignedAisle");
        Object bay = vars.get("assignedBay");
        Object level = vars.get("assignedLevel");
        Object position = vars.get("assignedPosition");
        if (aisle != null || bay != null || level != null || position != null) {
            return String.format("A%s-B%s-L%s-P%s",
                    valueOrDash(aisle), valueOrDash(bay), valueOrDash(level), valueOrDash(position));
        }
        return "BIN-A1";
    }

    private String valueOrDash(Object o) {
        return o == null ? "-" : o.toString();
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
