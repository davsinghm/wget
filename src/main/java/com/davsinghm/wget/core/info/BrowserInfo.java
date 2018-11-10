package com.davsinghm.wget.core.info;

import com.davsinghm.wget.Constants;

import java.net.URL;

public class BrowserInfo {

    private String userAgent = Constants.USER_AGENT;
    private URL referrer;

    public synchronized String getUserAgent() {
        return userAgent;
    }

    public synchronized void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public synchronized URL getReferrer() {
        return referrer;
    }

    public synchronized void setReferrer(URL referrer) {
        this.referrer = referrer;
    }
}
