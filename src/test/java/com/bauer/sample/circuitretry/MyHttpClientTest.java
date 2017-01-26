package com.bauer.sample.circuitretry;

import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

/**
 * Created by bauerb on 26/01/2017.
 */
public class MyHttpClientTest {
    final int WIREMOCK_PORT = 8089;
    final String WIREMOCK_URL = "http://localhost:" + WIREMOCK_PORT;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Test
    public void testExecuteRequestSuccess() throws Exception {
        Response response = new MyHttpClient().executeRequest(WIREMOCK_URL + "/success");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test(expected = ProcessingException.class) // ProcessingException is caused by SocketTimeoutException
    public void testExecuteRequestTimeoutWithoutRetry() throws Exception {
        new MyHttpClient().executeRequest(WIREMOCK_URL + "/timeout");
    }

    @Test
    public void testExecuteRequestTimeoutWithDefaultRetryPolicy() throws Exception {
        new MyHttpClient().executeRequest(WIREMOCK_URL + "/retry");
    }
}