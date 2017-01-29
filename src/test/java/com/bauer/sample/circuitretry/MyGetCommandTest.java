package com.bauer.sample.circuitretry;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.Predicate;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by bennybauer on 29/01/2017.
 */
public class MyGetCommandTest {
    final int WIREMOCK_PORT = 8089;
    final String WIREMOCK_URL = "http://localhost:" + WIREMOCK_PORT;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Test
    public void testSuccess() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/success");
            String result = getCommand.execute();

            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.SUCCESS)));
            assertEquals("OK", result);
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void testWithRetrySuccess() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/retry");
            String result = getCommand.execute();

            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.SUCCESS)));
            assertEquals("OK", result);
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void testFallbackNoRetry() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(0);
            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/noretry", retryPolicy);
            String result = getCommand.execute();

            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.TIMEOUT, HystrixEventType.FALLBACK_SUCCESS)));
            assertThat(getCommand.getExecutionException(), instanceOf(HystrixTimeoutException.class));
            assertEquals("Reached fallback", result);
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void testRetryExhausted() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(1);  // Only 1 retry, as stub retryX.json is configured to succeed on 3rd attempt
            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/retry", retryPolicy);
            String result = getCommand.execute();

            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.TIMEOUT, HystrixEventType.FALLBACK_SUCCESS)));
            assertThat(getCommand.getExecutionException(), instanceOf(HystrixTimeoutException.class));
            assertEquals("Reached fallback", result);
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void testRetryFailed() {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            RetryPolicy retryPolicy = new RetryPolicy()
                    .withDelay(1000, TimeUnit.MILLISECONDS)
                    .withMaxRetries(1)
                    .retryIf(new Predicate<Response>() {
                        public boolean test(Response response) {
                            return response != null && response.getStatus() == 429;
                        }
                    });
            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/throttlingfailed", retryPolicy);
            String result = getCommand.execute();

            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.FAILURE, HystrixEventType.FALLBACK_SUCCESS)));
            assertThat(getCommand.getExecutionException(), instanceOf(ClientErrorException.class));
            assertEquals("Reached fallback", result);
        } finally {
            context.shutdown();
        }
    }

//    @Test
//    public void testRetryExhaustedLongerThanCircuitTimeout() {
//        HystrixRequestContext context = HystrixRequestContext.initializeContext();
//        try {
//            RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(2).withDelay(6, TimeUnit.SECONDS);
//            MyGetCommand getCommand = new MyGetCommand(WIREMOCK_URL + "/retry", retryPolicy);
//            String result = getCommand.execute();
//
//            System.out.println("Request => " + HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
//            assertThat(getCommand.getExecutionEvents(), is(Arrays.asList(HystrixEventType.TIMEOUT, HystrixEventType.FALLBACK_SUCCESS)));
//            assertEquals("Reached fallback", result);
//        } finally {
//            context.shutdown();
//        }
//    }
}