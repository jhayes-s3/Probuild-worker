package com.probuild.worker.customer;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.probuild.worker.MessagePublisher;

import java.util.HashMap;
import java.util.Map;

@Component
public class SendPaymentInfoWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SendPaymentInfoWorker.class);

    private final MessagePublisher messagePublisher;

    public SendPaymentInfoWorker(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @JobWorker(type = "job-send-payment-info")
    public void sendPaymentInfo(final ActivatedJob job, final JobClient client) {

        LOGGER.info("job-send-payment-info fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();

        Object paymentReference = vars.get("paymentReference");

        if (paymentReference == null || paymentReference.toString().isBlank()) {
            paymentReference = "PAY-" + job.getKey();
        }

        LOGGER.info("Sending payment info to bank. paymentReference={}", paymentReference);

        messagePublisher.publish(
                "Message-payment-info",
                paymentReference.toString()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("paymentReference", paymentReference.toString());
        result.put("paymentInfoSent", true);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}