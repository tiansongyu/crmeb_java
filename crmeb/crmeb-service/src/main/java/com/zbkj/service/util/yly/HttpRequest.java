package com.zbkj.service.util.yly;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
class HttpRequest {
    public static String sendGet(String url, Map<String, String> paramMap) {
        String param = forMap(paramMap);
        StringBuilder result = new StringBuilder();
        String urlNameString = param.isEmpty() ? url : url + "?" + param;
        try {
            URLConnection connection = getUrlConnection(urlNameString);
            connection.connect();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            }
        } catch (Exception e) {
            log.warn("易联云GET请求失败", e);
        }
        return result.toString();
    }

    public static String sendPost(String url, Map<String, String> paramMap) {
        String param = forMap(paramMap);
        StringBuilder result = new StringBuilder();
        try {
            URLConnection conn = getUrlConnection(url);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            try (Writer out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                out.write(param);
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            }
        } catch (Exception e) {
            log.warn("易联云POST请求失败", e);
        }
        return result.toString();
    }

    private static URLConnection getUrlConnection(String url) throws IOException {
        URL realUrl = new URL(url);
        URLConnection conn = realUrl.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        return conn;
    }

    private static String forMap(Map<String, String> paramMap) {
        if (paramMap == null || paramMap.isEmpty()) {
            return "";
        }
        StringJoiner params = new StringJoiner("&");
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            params.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return params.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("UTF-8编码不可用", e);
        }
    }
}
