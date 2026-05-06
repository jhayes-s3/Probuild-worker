package com.probuild.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Stand-in for the supplier pool, which is non-executable in the BPMN.
 * Each endpoint publishes the message Probuild's executable process expects
 * to receive from the real supplier.
 */
@RestController
@RequestMapping("/sim/supplier")
public class SupplierSimulatorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierSimulatorController.class);

    @Autowired
    private MessagePublisher messagePublisher;

    @PostMapping("/start")
    public Map<String, String> startSupplierProcess(@RequestBody Map<String, String> body) {
        String purchaseOrderId = body.get("purchaseOrderId");
        if (purchaseOrderId == null || purchaseOrderId.isBlank()) {
            throw new IllegalArgumentException("purchaseOrderId is required");
        }
        String messageName = "Message-supplier-receives-po";
        LOGGER.info("Triggering supplier process for PO={}", purchaseOrderId);
        messagePublisher.publish(messageName, purchaseOrderId, Map.of("purchaseOrderId", purchaseOrderId));
        return Map.of(
                "status", "supplier process started",
                "messageName", messageName,
                "correlationKey", purchaseOrderId
        );
    }
}
