package org.servantframework.web;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private String version;
    private int statusCode;
    private String reasonPhrase;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    public HttpResponse(String version, int statusCode, String reasonPhrase) {
        this.version = version;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getVersion() {
        return version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setStatusCode(int i) {
        this.statusCode = i;
    }

    public void setReasonPhrase(String ok) {
        this.reasonPhrase = ok;
    }
}