package com.probuild.worker.customer;

import com.probuild.worker.MessagePublisher;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SendBankLogicWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SendBankLogicWorker.class);

    private final MessagePublisher messagePublisher;

    public SendBankLogicWorker(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @JobWorker(type = "job-send-bank-logic")
    public void sendBankLogic(final ActivatedJob job, final JobClient client) {

        LOGGER.info("job-send-bank-logic fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();

        String paymentReference = getString(vars, "paymentReference");

        if (paymentReference.isBlank()) {
            paymentReference = "PAY-" + job.getKey();
        }

        LOGGER.info("Sending bank payment response. paymentReference={}", paymentReference);

        messagePublisher.publish(
                "Message-bank-payment-response",
                paymentReference
        );

        Map<String, Object> result = new HashMap<>();
        result.put("bankResponseSent", true);
        result.put("paymentReference", paymentReference);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    private String getString(Map<String, Object> variables, String key) {
        Object value = variables.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }
}