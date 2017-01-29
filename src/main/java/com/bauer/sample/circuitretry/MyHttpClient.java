package com.bauer.sample.circuitretry;

import com.netflix.hystrix.exception.HystrixTimeoutException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.Predicate;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by bennybauer on 26/01/2017.
 */
public class MyHttpClient {
    final int CONNECTION_TIMEOUT = 1000;
    final int SOCKET_TIMEOUT = 500;
    final int HTTP_STATUS_CODE_TOO_MANY_REQUESTS = 429;

    private Client httpClient;
    private RetryPolicy retryPolicy;

    public MyHttpClient() {
        this(null);
    }

    public MyHttpClient(RetryPolicy retryPolicy) {
        if (retryPolicy != null) {
            this.retryPolicy = retryPolicy;
        } else {
            // Default retry policy
            this.retryPolicy = new RetryPolicy()
                    .retryOn(ProcessingException.class)
                    .withDelay(1000, TimeUnit.MILLISECONDS)
                    .withMaxRetries(2)
                    .retryIf(new Predicate<Response>() {
                        public boolean test(Response response) {
                            return response != null && response.getStatus() == HTTP_STATUS_CODE_TOO_MANY_REQUESTS;
                        }
                    });
        }

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, SOCKET_TIMEOUT);

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10);
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        httpClient = ClientBuilder.newClient(clientConfig);
    }

    public Response executeRequest(final String url) throws HystrixTimeoutException {
        try {
            Response response = Failsafe.with(retryPolicy).get(new Callable<Response>() {
                public Response call() {
                    System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " MyHttpClient: executing " + url);
                    return httpClient.target(url)
                            .request(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .get();
                }
            });

            System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " Returning: " + response.getStatus());

            if (response.getStatus() == HTTP_STATUS_CODE_TOO_MANY_REQUESTS) {
                System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " MyHttpClient exception: " + HTTP_STATUS_CODE_TOO_MANY_REQUESTS);
                throw new ClientErrorException(HTTP_STATUS_CODE_TOO_MANY_REQUESTS);
            }

            return response;
        } catch (ProcessingException e) {
            System.out.println(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " MyHttpClient exception: " + e.getMessage());
            throw new HystrixTimeoutException();
        }
    }
}
