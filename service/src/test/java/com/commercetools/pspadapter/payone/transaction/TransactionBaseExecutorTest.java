package com.commercetools.pspadapter.payone.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TransactionBaseExecutorTest {

    /**
     * Test {@code TransactionBaseExecutor#responseToJsonString} maps all input key-values to the result.
     */
    @Test
    public void responseToJsonString() throws Exception {
        String jsonString = TransactionBaseExecutor.responseToJsonString(ImmutableMap.of(
                "woot", "hack",
                "foo", "bar",
                "errorcode", "00123"
        ));

        assertThat(jsonString).isNotEmpty();

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.get("woot").asText()).isEqualTo("hack");
        assertThat(node.get("foo").asText()).isEqualTo("bar");
        assertThat(node.get("errorcode").asText()).isEqualTo("00123");

        // check no any other (default) fields are added
        assertThat(node.has(ERROR_MESSAGE)).isFalse();
        assertThat(node.has(CUSTOMER_MESSAGE)).isFalse();
        assertThat(node.has("redirecturl")).isFalse();
        assertThat(node.has("txid")).isFalse();
    }

    /**
     * Test {@code TransactionBaseExecutor.exceptionToResponseJsonString} for any exception returns standard Payone API
     * error fields set (status, errorcode, errormessage, customermessage)
     * @throws Exception
     */
    @Test
    public void exceptionToResponseJsonString() throws Exception {
        final String TEST_ERROR_MESSAGE = "TEST MESSAGE WOOT";
        String jsonString = TransactionBaseExecutor.exceptionToResponseJsonString(new Exception(TEST_ERROR_MESSAGE));

        assertThat(jsonString).isNotEmpty();

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.has(STATUS)).isTrue();
        assertThat(node.has(ERROR_CODE)).isTrue();
        assertThat(node.has(CUSTOMER_MESSAGE)).isTrue();

        assertThat(node.get(ERROR_MESSAGE).asText()).as("Error message node should contain exception message").contains(TEST_ERROR_MESSAGE);

    }

}
