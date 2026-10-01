package com.eventmanagment;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestHttpHelper {

    private TestHttpHelper() {
    }

    public static HttpURLConnection openConnection(
            String port,
            String path,
            String method) throws Exception {

        URL url = new URL("http://localhost:" + port + path);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setUseCaches(false);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        return connection;
    }

    public static void setJsonRequest(
            HttpURLConnection connection,
            String cookie) {

        connection.setRequestProperty(
                "Content-Type",
                "application/json"
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        String method =
                connection.getRequestMethod().toUpperCase();

        boolean safeMethod =
                method.equals("GET")
                        || method.equals("HEAD")
                        || method.equals("OPTIONS")
                        || method.equals("TRACE");

        if (safeMethod) {

            if (cookie != null && !cookie.isBlank()) {
                connection.setRequestProperty(
                        "Cookie",
                        cookie
                );
            }

        } else {

            try {
                String port =
                        String.valueOf(
                                connection.getURL().getPort()
                        );

                CsrfData csrfData =
                        getCsrfData(port, cookie);

                String mergedCookie =
                        mergeCookies(
                                cookie,
                                csrfData.cookie()
                        );

                if (mergedCookie != null
                        && !mergedCookie.isBlank()) {

                    connection.setRequestProperty(
                            "Cookie",
                            mergedCookie
                    );
                }

                connection.setRequestProperty(
                        "X-XSRF-TOKEN",
                        csrfData.token()
                );

            } catch (Exception ex) {

                throw new IllegalStateException(
                        "Unable to obtain CSRF token.",
                        ex
                );
            }
        }

        connection.setDoOutput(true);
    }

    public static void setCookie(
            HttpURLConnection connection,
            String cookie) {

        if (cookie == null || cookie.isBlank()) {
            return;
        }

        connection.setRequestProperty(
                "Cookie",
                cookie
        );
    }

    public static void writeBody(
            HttpURLConnection connection,
            String body) throws Exception {

        byte[] bytes =
                body.getBytes(StandardCharsets.UTF_8);

        try (OutputStream outputStream =
                     connection.getOutputStream()) {

            outputStream.write(bytes);
        }
    }

    public static String readResponseBody(
            HttpURLConnection connection) throws Exception {

        InputStream inputStream;

        if (connection.getResponseCode() >= 400) {
            inputStream = connection.getErrorStream();
        } else {
            inputStream = connection.getInputStream();
        }

        if (inputStream == null) {
            return "";
        }

        try (InputStream stream = inputStream) {

            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    public static Long extractLongField(
            String json,
            String fieldName) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(fieldName)
                        + "\"\\s*:\\s*(\\d+)"
        );

        Matcher matcher =
                pattern.matcher(json);

        if (matcher.find()) {
            return Long.parseLong(
                    matcher.group(1)
            );
        }

        return null;
    }

    public static String extractSessionCookie(
            HttpURLConnection connection) {

        return extractCookie(
                connection,
                "JSESSIONID"
        );
    }

    private static CsrfData getCsrfData(
            String port,
            String existingCookie) throws Exception {

        HttpURLConnection connection =
                openConnection(
                        port,
                        "/csrf",
                        "GET"
                );

        if (existingCookie != null
                && !existingCookie.isBlank()) {

            connection.setRequestProperty(
                    "Cookie",
                    existingCookie
            );
        }

        int status =
                connection.getResponseCode();

        String response =
                readResponseBody(connection);

        if (status != 200) {

            throw new AssertionError(
                    "Unable to obtain CSRF token. HTTP "
                            + status
                            + ", response: "
                            + response
            );
        }

        String token =
                extractJsonStringField(
                        response,
                        "token"
                );

        if (token == null || token.isBlank()) {

            throw new AssertionError(
                    "CSRF token missing from /csrf response: "
                            + response
            );
        }

        String csrfCookie =
                extractCookie(
                        connection,
                        "XSRF-TOKEN"
                );

        return new CsrfData(
                token,
                csrfCookie
        );
    }

    private static String extractJsonStringField(
            String json,
            String fieldName) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(fieldName)
                        + "\"\\s*:\\s*\"([^\"]*)\""
        );

        Matcher matcher =
                pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private static String extractCookie(
            HttpURLConnection connection,
            String cookieName) {

        Map<String, List<String>> headers =
                connection.getHeaderFields();

        if (headers == null) {
            return null;
        }

        for (Map.Entry<String, List<String>> entry
                : headers.entrySet()) {

            String headerName =
                    entry.getKey();

            if (headerName == null
                    || !headerName.equalsIgnoreCase(
                    "Set-Cookie")) {

                continue;
            }

            List<String> values =
                    entry.getValue();

            if (values == null) {
                continue;
            }

            for (String headerValue : values) {

                if (headerValue == null) {
                    continue;
                }

                String prefix =
                        cookieName + "=";

                if (!headerValue.startsWith(prefix)) {
                    continue;
                }

                String cookieValue =
                        headerValue.substring(
                                prefix.length()
                        );

                int semicolon =
                        cookieValue.indexOf(';');

                if (semicolon >= 0) {
                    cookieValue =
                            cookieValue.substring(
                                    0,
                                    semicolon
                            );
                }

                return cookieName
                        + "="
                        + cookieValue;
            }
        }

        return null;
    }

    private static String mergeCookies(
            String first,
            String second) {

        if (first == null || first.isBlank()) {
            return second;
        }

        if (second == null || second.isBlank()) {
            return first;
        }

        String result = first;

        String[] cookies =
                second.split(";");

        for (String cookie : cookies) {

            String trimmed =
                    cookie.trim();

            if (trimmed.isBlank()) {
                continue;
            }

            int equalsIndex =
                    trimmed.indexOf('=');

            if (equalsIndex <= 0) {
                continue;
            }

            String name =
                    trimmed.substring(
                            0,
                            equalsIndex
                    ).trim();

            result =
                    replaceCookie(
                            result,
                            name,
                            trimmed
                    );
        }

        return result;
    }

    private static String replaceCookie(
            String cookies,
            String cookieName,
            String newCookie) {

        StringBuilder result =
                new StringBuilder();

        boolean replaced = false;

        String[] existingCookies =
                cookies.split(";");

        for (String existing
                : existingCookies) {

            String trimmed =
                    existing.trim();

            if (trimmed.isBlank()) {
                continue;
            }

            int equalsIndex =
                    trimmed.indexOf('=');

            if (equalsIndex <= 0) {
                continue;
            }

            String existingName =
                    trimmed.substring(
                            0,
                            equalsIndex
                    ).trim();

            if (existingName.equalsIgnoreCase(
                    cookieName)) {

                if (!replaced) {

                    if (result.length() > 0) {
                        result.append("; ");
                    }

                    result.append(newCookie);
                    replaced = true;
                }

                continue;
            }

            if (result.length() > 0) {
                result.append("; ");
            }

            result.append(trimmed);
        }

        if (!replaced) {

            if (result.length() > 0) {
                result.append("; ");
            }

            result.append(newCookie);
        }

        return result.toString();
    }

    private record CsrfData(
            String token,
            String cookie) {
    }
}