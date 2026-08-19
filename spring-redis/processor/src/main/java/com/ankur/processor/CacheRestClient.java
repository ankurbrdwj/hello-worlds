package com.ankur.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Calls the external cache service via HTTP GET.
 * Zero external dependencies — works on Java 8+.
 */
public class CacheRestClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 30_000;

    private final String baseUrl;

    public CacheRestClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    /**
     * Returns the cached bytes for {@code key}, or {@code null} on a 404.
     * Throws {@link IOException} for any other non-200 response.
     */
    public byte[] get(String key) throws IOException {
        URL url = new URL(baseUrl + "/cache/" + key);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        try {
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Cache GET returned HTTP " + status + " for key=" + key);
            }
            return drain(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }
}