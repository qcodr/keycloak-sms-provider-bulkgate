/* Copyright 2026 qcodr and contributors. Licensed under the Apache License, Version 2.0. */
package io.github.qcodr.keycloak.bulkgate.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmsGatewayExceptionTest {

    @Test
    void carriesMessage() {
        SmsGatewayException e = new SmsGatewayException("boom");

        assertThat(e).hasMessage("boom").hasNoCause();
    }

    @Test
    void carriesMessageAndCause() {
        Throwable cause = new IllegalStateException("io");
        SmsGatewayException e = new SmsGatewayException("boom", cause);

        assertThat(e).hasMessage("boom").hasCause(cause);
    }
}
