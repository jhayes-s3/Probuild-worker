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
public class SendLoanInformationWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SendLoanInformationWorker.class);

    private final MessagePublisher messagePublisher;

    public SendLoanInformationWorker(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @JobWorker(type = "job-send-loan-information")
    public void sendLoanInformation(final ActivatedJob job, final JobClient client) {

        LOGGER.info("job-send-loan-information fired, Job ID: {}", job.getKey());
        LOGGER.info("All variables: {}", job.getVariablesAsMap());

        Map<String, Object> vars = job.getVariablesAsMap();

        String loanApplicationReference = getOrDefault(
                vars,
                "loanApplicationReference",
                "LOAN-" + job.getKey()
        );

        Map<String, Object> loanInformation = new HashMap<>();
        loanInformation.put("fullName", getOrDefault(vars, "fullName", ""));
        loanInformation.put("dateOfBirth", vars.get("dateOfBirth"));
        loanInformation.put("ssnOrItin", getOrDefault(vars, "ssnOrItin", ""));
        loanInformation.put("emailAddress", getOrDefault(vars, "emailAddress", ""));
        loanInformation.put("mobilePhoneNumber", getOrDefault(vars, "mobilePhoneNumber", ""));
        loanInformation.put("currentAddress", getOrDefault(vars, "currentAddress", ""));
        loanInformation.put("employmentStatus", vars.get("employmentStatus"));
        loanInformation.put("employerName", getOrDefault(vars, "employerName", ""));
        loanInformation.put("jobTitle", getOrDefault(vars, "jobTitle", ""));
        loanInformation.put("annualIncome", vars.get("annualIncome"));
        loanInformation.put("sourceOfIncome", getOrDefault(vars, "sourceOfIncome", ""));
        loanInformation.put("loanAmount", vars.get("loanAmount"));
        loanInformation.put("loanTerm", vars.get("loanTerm"));
        loanInformation.put("consentCreditCheck", getBooleanOrDefault(vars, "consentCreditCheck", false));
        loanInformation.put("agreeToTerms", getBooleanOrDefault(vars, "agreeToTerms", false));
        loanInformation.put("eSignConsent", getBooleanOrDefault(vars, "eSignConsent", false));
        loanInformation.put("privacyPolicyAck", getBooleanOrDefault(vars, "privacyPolicyAck", false));
        loanInformation.put("digitalSignature", getOrDefault(vars, "digitalSignature", ""));
        loanInformation.put("applicationDate", vars.get("applicationDate"));

        LOGGER.info(
                "Sending loan application information. loanApplicationReference={}, applicant={}, loanAmount={}",
                loanApplicationReference,
                loanInformation.get("fullName"),
                loanInformation.get("loanAmount")
        );

        messagePublisher.publish(
                "Message-loan-information",
                loanApplicationReference
        );

        Map<String, Object> result = new HashMap<>();
        result.put("loanApplicationReference", loanApplicationReference);
        result.put("loanInformationSent", true);
        result.put("loanInformation", loanInformation);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();
    }

    private String getOrDefault(Map<String, Object> vars, String key, String defaultValue) {
        Object value = vars.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value.toString().trim();
    }

    private boolean getBooleanOrDefault(
            Map<String, Object> vars,
            String key,
            boolean defaultValue
    ) {
        Object value = vars.get(key);

        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value.toString());
    }
}