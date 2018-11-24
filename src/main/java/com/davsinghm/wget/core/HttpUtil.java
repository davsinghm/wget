package com.davsinghm.wget.core;

import com.davsinghm.wget.core.info.ex.DownloadIOCodeError;
import com.davsinghm.wget.core.info.ex.DownloadMoved;

import java.io.IOException;
import java.net.HttpURLConnection;

public class HttpUtil {

    public static void checkResponse(HttpURLConnection c) throws IOException {
        int code = c.getResponseCode();
        switch (code) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
                return;
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                // the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user
                throw new DownloadMoved(c);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new DownloadIOCodeError(HttpURLConnection.HTTP_FORBIDDEN);
            case 416:
                // HTTP Error 416 - Requested Range Not Satisfiable
                throw new DownloadIOCodeError(416);
        }
    }
}
