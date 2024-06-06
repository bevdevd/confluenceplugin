package it.com.example.test.servlet.filter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

import static org.junit.Assert.*;


public class testFilterFuncTest {

    HttpClient httpClient;
    String baseUrl;

    @Before
    public void setup() {
        httpClient = new DefaultHttpClient();
        baseUrl = System.getProperty("baseurl");
    }

    @After
    public void tearDown() {
        httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void testSomething() throws IOException {
        HttpGet httpget = new HttpGet(baseUrl);

        // Create a response handler
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpClient.execute(httpget, responseHandler);
        assertTrue(null != responseBody && !"".equals(responseBody));
    }
}
