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
public class QueryStockRecordsWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryStockRecordsWorker.class);

    private static final String STOCK_URL = "http://localhost:8081/stock";
    private static final String STOCK_BY_TOOL_URL = "http://localhost:8081/stock/tool/";

    @Autowired
    private RestTemplate restTemplate;

    @JobWorker(type = "QueryStockRecords")
    public void queryStockRecords(final ActivatedJob job, final JobClient client) {
        LOGGER.info("QueryStockRecords fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Object toolIdVar = job.getVariablesAsMap().get("toolId");
        List<?> stockRecords;
        if (toolIdVar != null) {
            String url = STOCK_BY_TOOL_URL + toolIdVar;
            LOGGER.info("Querying stock for toolId={} via {}", toolIdVar, url);
            stockRecords = restTemplate.getForObject(url, List.class);
        } else {
            LOGGER.info("No toolId in variables; querying full stock list via {}", STOCK_URL);
            stockRecords = restTemplate.getForObject(STOCK_URL, List.class);
        }

        int recordCount = stockRecords != null ? stockRecords.size() : 0;
        boolean inStock = recordCount > 0;
        LOGGER.info("Stock records found: {}; setting inStock={}", recordCount, inStock);

        Map<String, Object> result = new HashMap<>();
        result.put("inStock", inStock);
        result.put("stockRecordCount", recordCount);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }
}
