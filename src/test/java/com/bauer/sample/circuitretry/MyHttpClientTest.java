package com.bauer.sample.circuitretry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.Predicate;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by bennybauer on 26/01/2017.
 */
public class MyHttpClientTest {
    final int WIREMOCK_PORT = 8089;
    final String WIREMOCK_URL = "http://localhost:" + WIREMOCK_PORT;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Test
    public void testExecuteRequestSuccess() throws HystrixTimeoutException {
        Response response = new MyHttpClient().executeRequest(WIREMOCK_URL + "/success");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test (expected = HystrixTimeoutException.class)
    public void testExecuteRequestTimeoutNoRetry() throws HystrixTimeoutException {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(0);
        Response response = new MyHttpClient(retryPolicy).executeRequest(WIREMOCK_URL + "/noretry");
        assertNull(response);
    }

    @Test
    public void testExecuteRequestTimeoutWithDefaultRetryPolicy() throws HystrixTimeoutException {
        Response response = new MyHttpClient().executeRequest(WIREMOCK_URL + "/retry");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    public void testExecuteRequest429WithDefaultRetryPolicy() throws HystrixTimeoutException {
        // TODO: there seems to be an issue with retrying "retryIf" more than once. Here I use it only once
        RetryPolicy retryPolicy = new RetryPolicy()
                .withDelay(1000, TimeUnit.MILLISECONDS)
                .withMaxRetries(1)
                .retryIf(new Predicate<Response>() {
                    public boolean test(Response response) {
                        return response != null && response.getStatus() == 429;
                    }
                });

        Response response = new MyHttpClient(retryPolicy).executeRequest(WIREMOCK_URL + "/throttling");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
}