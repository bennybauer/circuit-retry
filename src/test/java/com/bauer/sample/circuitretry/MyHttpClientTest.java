package com.bauer.sample.circuitretry;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.Predicate;
import org.apache.http.HttpStatus;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test(expected = HystrixTimeoutException.class)
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

    @Test (expected = ClientErrorException.class)
    public void testExecuteRequest429WithDefaultRetryPolicy() throws HystrixTimeoutException {
        RetryPolicy retryPolicy = new RetryPolicy()
                .withBackoff(500, 6000, TimeUnit.MILLISECONDS, 2)
                .withJitter(0.1)
                .withMaxRetries(2)
                .retryIf(new Predicate<Response>() {
                    public boolean test(Response response) {
                        return response != null && response.getStatus() == 429;
                    }
                });

        new MyHttpClient(retryPolicy).executeRequest(WIREMOCK_URL + "/throt");
    }

    @Test
    public void testRetryIfHangs() throws InterruptedException, ExecutionException, IOException {
        RetryPolicy retryPolicy = new RetryPolicy()
                .withDelay(500, TimeUnit.MILLISECONDS)
                .withMaxRetries(2)                          // works with one retry
                .retryIf(new Predicate<Response>() {
                    public boolean test(Response response) {
                        return response != null && response.getStatus() == 429;
                    }
                });

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(3);
        cm.setDefaultMaxPerRoute(3);

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        clientConfig.property(ClientProperties.READ_TIMEOUT, 500);
        clientConfig.connectorProvider(new ApacheConnectorProvider());   // works well when this line is commented out
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, cm);
        final Client httpClient = ClientBuilder.newClient(clientConfig);

        Response response = Failsafe.with(retryPolicy).get(new Callable<Response>() {
            public Response call() {
                String url = "http://httpstat.us/429";
                System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " Executing " + url);
                Response result = httpClient.target(url)
                        .request("text/plain")
                        .accept("text/plain")
                        .get();

                result.getEntity();
                return result;
            }
        });

        assertEquals(429, response.getStatus());
    }
}
