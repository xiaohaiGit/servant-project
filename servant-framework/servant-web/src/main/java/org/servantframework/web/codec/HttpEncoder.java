package org.servantframework.web.codec;

import org.servantframework.web.HttpResponse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpEncoder {

    public static ByteBuffer encode(HttpResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(response.getVersion())
                .append(" ")
                .append(response.getStatusCode())
                .append(" ")
                .append(response.getReasonPhrase())
                .append("\r\n");

        String body = response.getBody();
        if (body != null) {
            response.getHeaders().put("Content-Length", String.valueOf(response.getBody().getBytes(StandardCharsets.UTF_8).length));
        }
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            sb.append(header.getKey())
                    .append(": ")
                    .append(header.getValue())
                    .append("\r\n");
        }

        sb.append("\r\n");

        if (response.getBody() != null) {
            sb.append(response.getBody());
        }

        return ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}