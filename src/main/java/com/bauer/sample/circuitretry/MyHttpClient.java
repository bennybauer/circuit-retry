package com.bauer.sample.circuitretry;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by bennybauer on 26/01/2017.
 */
public class MyHttpClient {
    final int CONNECTION_TIMEOUT = 1000;
    final int SOCKET_TIMEOUT = 500;

    private Client httpClient;
    private RetryPolicy retryPolicy;

    public MyHttpClient() {
        // Default retry policy
        this(new RetryPolicy()
                .retryOn(ProcessingException.class)
                .withDelay(1, TimeUnit.SECONDS)
                .withMaxRetries(3));
    }
    public MyHttpClient(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        clientConfig.property(ClientProperties.READ_TIMEOUT, SOCKET_TIMEOUT);
        httpClient = ClientBuilder.newClient(clientConfig);
    }

    public Response executeRequest(final String url) throws Exception {
        Response response = Failsafe.with(retryPolicy).get(new Callable<Response>() {
            public Response call() {
                return httpClient.target(url)
                        .request(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .get();
            }
        });

        return response;
    }
}
