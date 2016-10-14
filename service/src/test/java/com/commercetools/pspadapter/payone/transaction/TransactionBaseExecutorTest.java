package com.commercetools.pspadapter.payone.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneResponseFields.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

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

        assertThat(jsonString, is(not(isEmptyOrNullString())));

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.get("woot").asText(), is("hack"));
        assertThat(node.get("foo").asText(), is("bar"));
        assertThat(node.get("errorcode").asText(), is("00123"));

        // check no any other (default) fields are added
        assertThat(node.has(ERROR_MESSAGE), is(false));
        assertThat(node.has(CUSTOMER_MESSAGE), is(false));
        assertThat(node.has("redirecturl"), is(false));
        assertThat(node.has("txid"), is(false));
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

        assertThat(jsonString, is(not(isEmptyOrNullString())));

        JsonNode node = new ObjectMapper().readTree(jsonString);

        assertThat(node.has(STATUS), is(true));
        assertThat(node.has(ERROR_CODE), is(true));
        assertThat(node.has(CUSTOMER_MESSAGE), is(true));

        assertThat("Error message node should contain exception message",
                node.get(ERROR_MESSAGE).asText(), containsString(TEST_ERROR_MESSAGE));

    }

}
