package com.probuild;

import com.probuild.worker.QueryStockRecordsWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueryStockRecordsWorkerTest {

    @Test
    void queryStockRecordsCompletesJobWithInStockTrueWhenRecordsExist() {
        ActivatedJob job = mock(ActivatedJob.class);
        JobClient client = mock(JobClient.class, RETURNS_DEEP_STUBS);
        RestTemplate restTemplate = mock(RestTemplate.class);

        when(job.getKey()).thenReturn(123L);
        when(job.getVariablesAsMap()).thenReturn(Map.of("toolId", 5));

        when(restTemplate.getForObject(anyString(), eq(List.class)))
                .thenReturn(List.of(Map.of("id", 1), Map.of("id", 2)));

        QueryStockRecordsWorker worker = new QueryStockRecordsWorker();
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);

        worker.queryStockRecords(job, client);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(client.newCompleteCommand(123L)).variables(captor.capture());

        assertEquals(true, captor.getValue().get("inStock"));
        assertEquals(2, captor.getValue().get("stockRecordCount"));
    }
}