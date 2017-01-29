package com.bauer.sample.circuitretry;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.core.Response;

/**
 * Created by bennybauer on 26/01/2017.
 */
public class MyGetCommand extends HystrixCommand<String> {
    private String url;
    private MyHttpClient client;

    public MyGetCommand(String url) {
        this(url, null);
    }

    public MyGetCommand(String url, RetryPolicy retryPolicy) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MyGet"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutEnabled(false)));
        this.url = url;
        client = new MyHttpClient(retryPolicy);
    }

    @Override
    protected String run() throws HystrixTimeoutException {
        Response response = client.executeRequest(url);
        String result = response.getStatusInfo().toString();
        response.close();
        return result;
    }

    @Override
    protected String getFallback() {
        System.out.println("getFallback: " + this.isCommandTimedOut);
        return "Reached fallback";
    }
}
