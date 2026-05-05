package com.probuild.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CheckStockAvailabilityWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckStockAvailabilityWorker.class);

    private static final String STOCK_URL = "http://localhost:8081/stock";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "job-check-stock-avaliability")
    public void checkStockAvailability(final ActivatedJob job, final JobClient client) {
        LOGGER.info("job-check-stock-avaliability fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object purchaseOrderId = job.getVariable("purchaseOrderId");
        LOGGER.info("Checking stock for purchaseOrderId: {}", purchaseOrderId);

        List<?> stock = restTemplate.getForObject(STOCK_URL, List.class);
        int stockRecordCount = stock != null ? stock.size() : 0;
        LOGGER.info("Total stock records found: {}", stockRecordCount);

        Map<String, Object> result = new HashMap<>();
        result.put("stockChecked", true);
        result.put("stockRecordCount", stockRecordCount);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
