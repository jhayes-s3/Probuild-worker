package com.probuild;

import com.probuild.worker.MessagePublisher;
import com.probuild.worker.SupplierSimulatorController;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SupplierSimulatorControllerTest {

    @Test
    void startSupplierProcessPublishesMessageAndReturnsStatus() {
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        SupplierSimulatorController controller = new SupplierSimulatorController();
        ReflectionTestUtils.setField(controller, "messagePublisher", messagePublisher);

        Map<String, String> response = controller.startSupplierProcess(
                Map.of("purchaseOrderId", "PO-123")
        );

        verify(messagePublisher).publish(
                "Message-supplier-receives-po",
                "PO-123",
                Map.of("purchaseOrderId", "PO-123")
        );

        assertEquals("supplier process started", response.get("status"));
        assertEquals("Message-supplier-receives-po", response.get("messageName"));
        assertEquals("PO-123", response.get("correlationKey"));
    }

    @Test
    void startSupplierProcessThrowsWhenPurchaseOrderIdMissing() {
        MessagePublisher messagePublisher = mock(MessagePublisher.class);

        SupplierSimulatorController controller = new SupplierSimulatorController();
        ReflectionTestUtils.setField(controller, "messagePublisher", messagePublisher);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.startSupplierProcess(Map.of())
        );

        verifyNoInteractions(messagePublisher);
    }
}