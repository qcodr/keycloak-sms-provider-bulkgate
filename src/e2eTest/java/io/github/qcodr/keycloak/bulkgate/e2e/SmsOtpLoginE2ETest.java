/*
 * Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0.
 */
package io.github.qcodr.keycloak.bulkgate.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of the full login: a real Keycloak 26.6.3 with the provider jar
 * deployed, BulkGate mocked by WireMock, driven through the browser login pages
 * with HtmlUnit.
 *
 * <p>Proves the whole chain works together — password step, OTP issuance, SMS
 * dispatch to (mocked) BulkGate, and local verification of the code the user
 * "received".</p>
 */
class SmsOtpLoginE2ETest {

    private static final String REALM = "bulkgate-e2e";
    private static final String CLIENT = "e2e-client";
    private static final String FLOW = "bulkgate-browser";
    private static final String USERNAME = "alice";
    private static final String PASSWORD = "password";
    private static final String PHONE = "+36201234567";
    private static final String SMS_PATH = "/api/1.0/advanced/transactional";
    private static final Pattern CODE_PATTERN = Pattern.compile("(\\d{6})");

    private static final Network NETWORK = Network.newNetwork();

    private static final GenericContainer<?> WIREMOCK = new GenericContainer<>("wiremock/wiremock:3.9.2")
            .withNetwork(NETWORK)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .withClasspathResourceMapping(
                    "wiremock/mappings/transactional.json",
                    "/home/wiremock/mappings/transactional.json",
                    org.testcontainers.containers.BindMode.READ_ONLY)
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

    private static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.6.3")
            .withProviderLibsFrom(List.of(new File(System.getProperty("provider.jar"))))
            .withNetwork(NETWORK)
            .withNetworkAliases("keycloak");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void startAndConfigure() {
        WIREMOCK.start();
        KEYCLOAK.start();
        configureRealm();
    }

    @AfterAll
    static void stop() {
        KEYCLOAK.stop();
        WIREMOCK.stop();
    }

    @BeforeEach
    void resetJournal() throws Exception {
        HTTP.send(HttpRequest.newBuilder(URI.create(wiremockAdmin() + "/__admin/requests"))
                .DELETE().build(), HttpResponse.BodyHandlers.discarding());
    }

    @Test
    void userLogsInWithPasswordThenSmsCode() throws Exception {
        try (WebClient web = newWebClient()) {
            HtmlPage otpPage = submitPassword(web);
            assertThat(otpPage.getElementById("code")).as("OTP form should be shown").isNotNull();

            // Guard the instruction line: the TTL must render as a number, not a
            // leaked FreeMarker formatter object.
            String otpText = otpPage.asNormalizedText();
            assertThat(otpText).doesNotContain("freemarker", "NumberFormatter");
            assertThat(otpText).containsPattern("valid for \\d+ minutes");

            String code = latestSentCode();
            org.htmlunit.Page result = submitCode(otpPage, code);

            assertThat(result.getUrl().toString())
                    .as("successful login redirects back with an authorization code")
                    .contains("code=");

            // A successful OTP stamps the standard phone-verified attribute.
            assertThat(userAttribute(USERNAME, "phoneNumberVerified"))
                    .as("phone marked verified after successful OTP")
                    .isEqualTo("true");
        }
    }

    @Test
    void wrongCodeKeepsUserOnOtpFormWithError() throws Exception {
        try (WebClient web = newWebClient()) {
            HtmlPage otpPage = submitPassword(web);
            latestSentCode(); // ensure an SMS was issued

            org.htmlunit.Page result = submitCode(otpPage, "000000");

            assertThat(result).isInstanceOf(HtmlPage.class);
            HtmlPage htmlResult = (HtmlPage) result;
            assertThat(htmlResult.getElementById("code")).as("still on the OTP form").isNotNull();
            assertThat(htmlResult.asNormalizedText()).contains("Invalid verification code");
        }
    }

    // ---------------------------------------------------------------- flow ---

    private HtmlPage submitPassword(WebClient web) throws Exception {
        HtmlPage loginPage = web.getPage(authorizationUrl());
        ((HtmlInput) loginPage.getElementById("username")).setValue(USERNAME);
        ((HtmlInput) loginPage.getElementById("password")).setValue(PASSWORD);
        org.htmlunit.Page next = ((HtmlElement) loginPage.getElementById("kc-login")).click();
        if (!(next instanceof HtmlPage page)) {
            throw new AssertionError("Expected HTML after password submit but got "
                    + next.getWebResponse().getContentType() + " (status "
                    + next.getWebResponse().getStatusCode() + "):\n"
                    + next.getWebResponse().getContentAsString());
        }
        return page;
    }

    private org.htmlunit.Page submitCode(HtmlPage otpPage, String code) throws Exception {
        ((HtmlInput) otpPage.getElementById("code")).setValue(code);
        // The submit is a PF5 <button id="kc-login">, rendered by the buttons.ftl macro.
        return ((HtmlElement) otpPage.getElementById("kc-login")).click();
    }

    private String authorizationUrl() {
        String redirectUri = wiremockAdmin() + "/cb";
        return KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/auth"
                + "?client_id=" + CLIENT
                + "&response_type=code&scope=openid"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }

    private String latestSentCode() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(wiremockAdmin() + "/__admin/requests")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode requests = MAPPER.readTree(response.body()).path("requests");
        for (JsonNode entry : requests) {
            JsonNode request = entry.path("request");
            if ("POST".equals(request.path("method").asText()) && request.path("url").asText().contains(SMS_PATH)) {
                JsonNode smsBody = MAPPER.readTree(request.path("body").asText());
                Matcher matcher = CODE_PATTERN.matcher(smsBody.path("text").asText());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        throw new AssertionError("No OTP SMS captured by WireMock");
    }

    private static WebClient newWebClient() {
        WebClient web = new WebClient();
        web.getOptions().setJavaScriptEnabled(false);
        web.getOptions().setCssEnabled(false);
        web.getOptions().setThrowExceptionOnFailingStatusCode(false);
        web.getOptions().setThrowExceptionOnScriptError(false);
        web.getOptions().setRedirectEnabled(true);
        return web;
    }

    private static String wiremockAdmin() {
        return "http://" + WIREMOCK.getHost() + ":" + WIREMOCK.getMappedPort(8080);
    }

    // ------------------------------------------------------- realm set-up ----

    private static void configureRealm() {
        try (Keycloak admin = KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(KEYCLOAK.getAdminUsername())
                .password(KEYCLOAK.getAdminPassword())
                .build()) {

            RealmRepresentation realmRep = new RealmRepresentation();
            realmRep.setRealm(REALM);
            realmRep.setEnabled(true);
            admin.realms().create(realmRep);

            RealmResource realm = admin.realm(REALM);
            enableUnmanagedAttributes(realm);
            createUser(realm);
            createClient(realm);
            createFlow(realm);

            RealmRepresentation toUpdate = realm.toRepresentation();
            toUpdate.setBrowserFlow(FLOW);
            realm.update(toUpdate);
        }
    }

    private static void enableUnmanagedAttributes(RealmResource realm) {
        // Keycloak's declarative user profile otherwise discards attributes that
        // are not declared, including the phone number.
        org.keycloak.representations.userprofile.config.UPConfig profile =
                realm.users().userProfile().getConfiguration();
        profile.setUnmanagedAttributePolicy(
                org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy.ENABLED);
        realm.users().userProfile().update(profile);
    }

    private static void createUser(RealmResource realm) {
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(PASSWORD);
        password.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(USERNAME);
        user.setEnabled(true);
        user.setEmail("alice@example.com");
        user.setEmailVerified(true);
        user.setFirstName("Alice");
        user.setLastName("Test");
        user.setCredentials(List.of(password));
        user.setAttributes(Map.of("phoneNumber", List.of(PHONE)));
        realm.users().create(user).close();
    }

    private static void createClient(RealmResource realm) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT);
        client.setEnabled(true);
        client.setPublicClient(true);
        client.setStandardFlowEnabled(true);
        client.setRedirectUris(List.of("*"));
        client.setWebOrigins(List.of("*"));
        realm.clients().create(client).close();
    }

    private static void createFlow(RealmResource realm) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(FLOW);
        flow.setDescription("Password + BulkGate SMS OTP");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        realm.flows().createFlow(flow);

        realm.flows().addExecution(FLOW, Map.<String, Object>of("provider", "auth-username-password-form"));
        realm.flows().addExecution(FLOW, Map.<String, Object>of("provider", "bulkgate-sms-otp-authenticator"));

        for (AuthenticationExecutionInfoRepresentation execution : realm.flows().getExecutions(FLOW)) {
            execution.setRequirement("REQUIRED");
            realm.flows().updateExecutions(FLOW, execution);
        }

        AuthenticationExecutionInfoRepresentation smsExecution = realm.flows().getExecutions(FLOW).stream()
                .filter(e -> "bulkgate-sms-otp-authenticator".equals(e.getProviderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SMS OTP execution not found"));

        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("bulkgate-config");
        config.setConfig(bulkGateConfig());
        realm.flows().newExecutionConfig(smsExecution.getId(), config).close();
    }

    private static Map<String, String> bulkGateConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("bulkgateApiUrl", "http://wiremock:8080" + SMS_PATH);
        config.put("simulationMode", "false");
        config.put("bulkgateApplicationId", "e2e-app");
        config.put("bulkgateApplicationToken", "e2e-token");
        config.put("bulkgateSenderId", "gText");
        config.put("bulkgateSenderIdValue", "Keycloak");
        config.put("codeLength", "6");
        config.put("codeTtlSeconds", "300");
        config.put("maxVerifyAttempts", "3");
        config.put("resendCooldownSeconds", "1");
        config.put("maxResends", "3");
        config.put("phoneNumberAttribute", "phoneNumber");
        config.put("defaultCountryCode", "+36");
        return config;
    }

    /** Reads a single user attribute via a short-lived admin client. */
    private static String userAttribute(String username, String attribute) {
        try (Keycloak admin = KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username(KEYCLOAK.getAdminUsername())
                .password(KEYCLOAK.getAdminPassword())
                .build()) {
            UserRepresentation user = admin.realm(REALM).users().search(username).get(0);
            Map<String, List<String>> attrs = user.getAttributes();
            if (attrs == null || attrs.get(attribute) == null || attrs.get(attribute).isEmpty()) {
                return null;
            }
            return attrs.get(attribute).get(0);
        }
    }
}
