/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends SMS through the BulkGate Advanced Transactional API.
 *
 * <p>Speaks the documented JSON contract: credentials and message in the body,
 * {@code {"data":{"status":"accepted","sms_id":...}}} on success, and
 * {@code {"type":...,"code":...,"error":...}} on rejection. Transport problems
 * (no response, timeout, unreadable success body) surface as
 * {@link SmsGatewayException}; a provider verdict surfaces as
 * {@link SmsSendResult}.</p>
 *
 * @see <a href="https://help.bulkgate.com/docs/en/http-advanced-transactional.html">BulkGate Advanced Transactional API</a>
 */
public class BulkGateSmsGateway implements SmsGateway {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final BulkGateSettings settings;
    private final Duration requestTimeout;

    public BulkGateSmsGateway(
            HttpClient httpClient, ObjectMapper mapper, BulkGateSettings settings, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.settings = settings;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public SmsSendResult send(SmsMessage message) throws SmsGatewayException {
        settings.requireCredentials();
        String payload = serialize(message);
        HttpResponse<String> response = dispatch(payload);
        return interpret(response);
    }

    private String serialize(SmsMessage message) throws SmsGatewayException {
        ObjectNode body = mapper.createObjectNode();
        body.put("application_id", settings.applicationId());
        body.put("application_token", settings.applicationToken());
        body.put("number", numberForApi(message.recipient()));
        body.put("text", message.text());
        body.put("unicode", settings.unicode());
        body.put("sender_id", settings.senderId());
        if (settings.hasSenderIdValue()) {
            body.put("sender_id_value", settings.senderIdValue());
        }
        if (settings.hasCountry()) {
            body.put("country", settings.country());
        }
        try {
            return mapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new SmsGatewayException("Failed to serialize BulkGate request", e);
        }
    }

    private HttpResponse<String> dispatch(String payload) throws SmsGatewayException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.apiUrl()))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SmsGatewayException("BulkGate request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmsGatewayException("BulkGate request interrupted", e);
        }
    }

    private SmsSendResult interpret(HttpResponse<String> response) throws SmsGatewayException {
        int status = response.statusCode();
        JsonNode root = parseJson(response.body(), status);

        if (isSuccess(status)) {
            JsonNode data = root.path("data");
            String reportedStatus = data.path("status").asText("");
            if ("accepted".equals(reportedStatus)) {
                return SmsSendResult.accepted(data.path("sms_id").asText(null));
            }
            if (root.has("error")) {
                return SmsSendResult.rejected(
                        errorCode(root, status), root.path("error").asText());
            }
            throw new SmsGatewayException("Unexpected BulkGate success payload (status='" + reportedStatus + "')");
        }
        return SmsSendResult.rejected(errorCode(root, status), errorMessage(root, response.body()));
    }

    private JsonNode parseJson(String body, int status) throws SmsGatewayException {
        try {
            return mapper.readTree(body == null ? "" : body);
        } catch (IOException e) {
            if (isSuccess(status)) {
                // A 2xx we cannot read is not a safe "delivered" — treat as transport failure.
                throw new SmsGatewayException("Unreadable BulkGate response body", e);
            }
            return mapper.createObjectNode();
        }
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static String errorCode(JsonNode root, int status) {
        if (root.hasNonNull("code")) {
            return root.path("code").asText();
        }
        if (root.hasNonNull("type")) {
            return root.path("type").asText();
        }
        return "http_" + status;
    }

    private static final int MAX_ERROR_BODY_CHARS = 200;

    private static String errorMessage(JsonNode root, String rawBody) {
        if (root.hasNonNull("error")) {
            return root.path("error").asText();
        }
        if (rawBody == null || rawBody.isBlank()) {
            return "BulkGate rejected the request";
        }
        // Cap an unstructured body so a misconfigured endpoint can't flood the log
        // (or echo request fragments back into it).
        return rawBody.length() <= MAX_ERROR_BODY_CHARS ? rawBody : rawBody.substring(0, MAX_ERROR_BODY_CHARS) + "…";
    }

    private static String numberForApi(String recipient) {
        // BulkGate expects digits only (e.g. "420777123456"), no leading '+'.
        return recipient.startsWith("+") ? recipient.substring(1) : recipient;
    }
}
