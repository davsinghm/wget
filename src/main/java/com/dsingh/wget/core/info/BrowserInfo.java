package com.dsingh.wget.core.info;

import com.dsingh.wget.Constants;

import java.net.URL;

public class BrowserInfo {

    private String userAgent = Constants.USER_AGENT;
    private URL referrer;

    synchronized public String getUserAgent() {
        return userAgent;
    }

    synchronized public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    synchronized public URL getReferrer() {
        return referrer;
    }

    synchronized public void setReferrer(URL referrer) {
        this.referrer = referrer;
    }
}
