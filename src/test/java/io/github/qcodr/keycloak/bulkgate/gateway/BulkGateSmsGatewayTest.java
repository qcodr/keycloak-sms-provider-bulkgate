/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulkGateSmsGatewayTest {

    private static final String PATH = "/api/1.0/advanced/transactional";

    @RegisterExtension
    static final WireMockExtension WM = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private BulkGateSmsGateway gatewayWith(BulkGateSettings settings) {
        return new BulkGateSmsGateway(httpClient, mapper, settings, Duration.ofSeconds(5));
    }

    private BulkGateSettings liveSettings() {
        return new BulkGateSettings(WM.baseUrl() + PATH, "app-1", "token-1", "gText", "Keycloak", false, "");
    }

    @Test
    void returnsAcceptedAndParsesSmsId() throws Exception {
        WM.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(
                "{\"data\":{\"status\":\"accepted\",\"sms_id\":\"sms-123\",\"number\":\"36201234567\"}}")));

        SmsSendResult result = gatewayWith(liveSettings())
                .send(new SmsMessage("+36201234567", "Your code is 123456"));

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("sms-123");
    }

    @Test
    void sendsCredentialsNumberWithoutPlusAndText() throws Exception {
        WM.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(
                "{\"data\":{\"status\":\"accepted\",\"sms_id\":\"sms-123\"}}")));

        gatewayWith(liveSettings()).send(new SmsMessage("+36201234567", "Your code is 123456"));

        WM.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(matchingJsonPath("$.application_id", equalTo("app-1")))
                .withRequestBody(matchingJsonPath("$.application_token", equalTo("token-1")))
                .withRequestBody(matchingJsonPath("$.number", equalTo("36201234567")))
                .withRequestBody(matchingJsonPath("$.text", equalTo("Your code is 123456")))
                .withRequestBody(matchingJsonPath("$.sender_id", equalTo("gText")))
                .withRequestBody(matchingJsonPath("$.sender_id_value", equalTo("Keycloak"))));
    }

    @Test
    void mapsProviderErrorToRejectedResult() throws Exception {
        WM.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"type\":\"invalid_input_parameters\",\"code\":400,"
                        + "\"error\":\"Wrong phone number\",\"detail\":null}")));

        SmsSendResult result = gatewayWith(liveSettings())
                .send(new SmsMessage("+36201234567", "Your code is 123456"));

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo("400");
        assertThat(result.errorMessage()).isEqualTo("Wrong phone number");
    }

    @Test
    void throwsGatewayExceptionOnTransportFault() {
        WM.stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> gatewayWith(liveSettings())
                .send(new SmsMessage("+36201234567", "Your code is 123456")))
                .isInstanceOf(SmsGatewayException.class);
    }

    @Test
    void refusesToSendWithoutCredentials() {
        BulkGateSettings noCreds = new BulkGateSettings(WM.baseUrl() + PATH, "", "", "gSystem", "", false, "");

        assertThatThrownBy(() -> gatewayWith(noCreds)
                .send(new SmsMessage("+36201234567", "Your code is 123456")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void omitsSenderIdValueAndCountryWhenBlank() throws Exception {
        WM.stubFor(post(urlEqualTo(PATH)).willReturn(okJson(
                "{\"data\":{\"status\":\"accepted\",\"sms_id\":\"sms-9\"}}")));
        BulkGateSettings minimal =
                new BulkGateSettings(WM.baseUrl() + PATH, "app-1", "token-1", "gSystem", "", false, "");

        gatewayWith(minimal).send(new SmsMessage("+36201234567", "Hi"));

        List<LoggedRequest> requests = WM.findAll(postRequestedFor(urlEqualTo(PATH)));
        assertThat(requests).hasSize(1);
        JsonNode body = mapper.readTree(requests.get(0).getBodyAsString());
        assertThat(body.get("sender_id").asText()).isEqualTo("gSystem");
        assertThat(body.has("sender_id_value")).isFalse();
        assertThat(body.has("country")).isFalse();
    }
}
