package com.commercetools.pspadapter.payone.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.commercetools.pspadapter.payone.transaction.TransactionBaseExecutor.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TransactionBaseExecutorTest {

    @Test
    public void responseToJsonString_mapsAllValuesFromResponse() throws Exception {
        String jsonString = TransactionBaseExecutor.responseToJsonString(ImmutableMap.of(
                "woot", "hack",
                "foo", "bar",
                "errorcode", "00123"
        ));

        assertThat(jsonString, is(not(isEmptyOrNullString())));

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.get("woot").asText(), is("hack"));
        assertThat(node.get("foo").asText(), is("bar"));
        assertThat(node.get("errorcode").asText(), is("00123"));

        // check no any other fields are added
        assertThat(node.has(ERROR_MESSAGE), is(false));
        assertThat(node.has(CUSTOMER_MESSAGE), is(false));
        assertThat(node.has("redirecturl"), is(false));
        assertThat(node.has("txid"), is(false));
    }

    @Test
    public void exceptionToResponseJsonString_containsPayoneApiNodes() throws Exception {
        final String TEST_ERROR_MESSAGE = "TEST MESSAGE WOOT";
        String jsonString = TransactionBaseExecutor.exceptionToResponseJsonString(new Exception(TEST_ERROR_MESSAGE));

        assertThat(jsonString, is(not(isEmptyOrNullString())));

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.has(STATUS), is(true));
        assertThat(node.has(ERROR_CODE), is(true));
        assertThat(node.has(CUSTOMER_MESSAGE), is(true));

        assertThat("Error message node should contain exception message",
                node.get(ERROR_MESSAGE).asText(), containsString(TEST_ERROR_MESSAGE));

    }

}
