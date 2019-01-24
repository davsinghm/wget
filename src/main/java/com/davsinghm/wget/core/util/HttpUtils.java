package com.davsinghm.wget.core.util;

import com.davsinghm.wget.Constants;
import com.davsinghm.wget.core.info.DownloadInfo;
import com.davsinghm.wget.core.info.ex.DownloadIOCodeException;
import com.davsinghm.wget.core.info.ex.DownloadMoved;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtils {

    public static void checkResponse(HttpURLConnection c) throws IOException {

        switch (c.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
                return;
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                // the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user
                throw new DownloadMoved(c);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new DownloadIOCodeException(HttpURLConnection.HTTP_FORBIDDEN, "HTTP 403: Forbidden. URL: " + c.getURL().toExternalForm());
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new DownloadIOCodeException(HttpURLConnection.HTTP_NOT_FOUND, "HTTP 404: Not Found. URL: " + c.getURL().toExternalForm());
            case 416:
                // HTTP Error 416 - Requested Range Not Satisfiable
                throw new DownloadIOCodeException(416, "HTTP 416: Requested Range Not Satisfiable. URL: " + c.getURL().toExternalForm());
        }
    }

    public static HttpURLConnection openConnection(DownloadInfo info, long start, long end) throws IOException {

        URL url = info.getSource();

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

        urlConnection.setRequestProperty("User-Agent", info.getUserAgent());

        if (info.getReferer() != null)
            urlConnection.setRequestProperty("Referer", info.getReferer().toExternalForm());

        if (start > 0 || end > 0)
            urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + (end > 0 ? end : ""));

        return urlConnection;
    }

    public static HttpURLConnection openConnection(DownloadInfo info, long start) throws IOException {
        return openConnection(info, start, 0);
    }
}
