package com.eppo.sdk.helpers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Eppo Http Client Class
 */
public class EppoHttpClient {
    private HttpClient httpClient = HttpClient.newHttpClient();
    private Map<String, String> defaultParams = new HashMap<>();
    private String baseURl;

    private int requestTimeOutMillis = 3000; // 3 secs

    public EppoHttpClient(String apikey, String sdkName, String sdkVersion, String baseURl) {
        this.defaultParams.put("apiKey", apikey);
        this.defaultParams.put("sdkName", sdkName);
        this.defaultParams.put("sdkVersion", sdkVersion);
        this.baseURl = baseURl;
    }

    public EppoHttpClient(
            String apikey,
            String sdkName,
            String sdkVersion,
            String baseUrl,
            int requestTimeOutMillis
    ) {
        this.defaultParams.put("apiKey", apikey);
        this.defaultParams.put("sdkName", sdkName);
        this.defaultParams.put("sdkVersion", sdkVersion);
        this.baseURl = baseUrl;
        this.requestTimeOutMillis = requestTimeOutMillis;
    }


    /**
     * This function is used to add default query parameters
     *
     * @param key
     * @param value
     */
    public void addDefaultParam(String key, String value) {
        this.defaultParams.put(key, value);
    }

    /**
     * This function is used to make get request
     *
     * @param url
     * @param params
     * @param headers
     * @return
     * @throws Exception
     */
    public HttpResponse<String> get(
            String url,
            Map<String, String> params,
            Map<String, String> headers
    ) throws Exception {
        // Merge both the query parameters
        Map<String, String> allParams = Stream.of(params, this.defaultParams)
                .flatMap(map -> map.entrySet().stream())
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> value1)
                );

        // Build URL
        final String newUrl = this.urlBuilder(this.baseURl + url, allParams);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofMillis(requestTimeOutMillis))
                .uri(new URI(newUrl));

        // Set Header
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder = builder.setHeader(entry.getKey(), entry.getValue());
        }

        HttpRequest request = builder.build();

        // Make a Request
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * This function is used to make a get request
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public HttpResponse<String> get(String url, Map<String, String> params) throws Exception {
        return this.get(url, params, new HashMap<>());
    }

    /**
     * This function is used to make get request
     *
     * @param url
     * @return
     * @throws Exception
     */
    public HttpResponse<String> get(String url) throws Exception {
        return this.get(url, new HashMap<>(), new HashMap<>());
    }

    /**
     * This function is used to build url with query parameters
     *
     * @param url
     * @param params
     * @return
     */
    public String urlBuilder(String url, Map<String, String> params) {
        StringBuilder newUrlBuilder = new StringBuilder(url);
        boolean isFirst = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (isFirst) {
                newUrlBuilder.append("?");
            }
            isFirst = false;
            newUrlBuilder.append(entry.getKey());
            newUrlBuilder.append("=");
            newUrlBuilder.append(entry.getValue());
            newUrlBuilder.append("&");
        }

        return newUrlBuilder.toString();
    }
}
