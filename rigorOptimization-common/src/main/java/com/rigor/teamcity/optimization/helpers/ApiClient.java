package com.rigor.teamcity.optimization.helpers;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class ApiClient {

    public static HttpResponse Get(String url, Header[] headers) {

        HttpResponse httpResponse = null;

        try {

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);

            if (headers != null)
            {
                // Set any passed headers.
                request.setHeaders(headers);
            }

            httpResponse = httpClient.execute(request);

        } catch (Exception e) {
            // Internal server error occurred - set the status code and capture the exception message into the response object.
            httpResponse.setStatusCode(500);
            httpResponse.setReasonPhrase(e.getMessage());
        }

        return httpResponse;
    }
}