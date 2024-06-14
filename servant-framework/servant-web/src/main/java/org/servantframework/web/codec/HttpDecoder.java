package org.servantframework.web.codec;

import org.servantframework.web.HttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HttpDecoder {
    public static HttpRequest decode(String rawRequest) {
        HttpRequest request = new HttpRequest();
        String[] lines = rawRequest.split("\r\n", -1);
        String[] requestLine = lines[0].split(" ");

        request.setMethod(requestLine[0]);
        String fullPath = requestLine[1];
        String path = fullPath.split("\\?")[0];
        request.setPath(path);

        if (fullPath.contains("?")) {
            String queryString = fullPath.split("\\?")[1];
            request.setParameters(parseParameters(queryString));
        }

        request.setProtocol(requestLine[2]);

        int i = 1;
        while (!lines[i].isEmpty()) {
            String[] header = lines[i].split(": ");
            request.getHeaders().put(header[0], header[1]);
            i++;
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            StringBuilder bodyBuilder = new StringBuilder();
            for (int j = i + 1; j < lines.length; j++) {
                bodyBuilder.append(lines[j]);
            }
            String body = bodyBuilder.toString();
            request.setBody(body);

            String contentType = request.getHeaders().get("Content-Type");
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                request.setParameters(parseParameters(body));
            }
        }

        return request;
    }

    private static Map<String, String> parseParameters(String paramString) {
        Map<String, String> parameters = new HashMap<>();
        String[] pairs = paramString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            try {
                String key = URLDecoder.decode(keyValue[0], "UTF-8");
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], "UTF-8") : "";
                parameters.put(key, value);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return parameters;
    }
}
