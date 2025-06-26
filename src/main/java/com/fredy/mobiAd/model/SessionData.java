package com.fredy.mobiAd.model;

import java.io.Serializable;

public class SessionData implements Serializable {

    private String ussdPath;
    private long lastActivityTime;

    public SessionData() {}

    public SessionData(String ussdPath, long lastActivityTime) {
        this.ussdPath = ussdPath;
        this.lastActivityTime = lastActivityTime;
    }

    // Getters and Setters
    public String getUssdPath() {
        return ussdPath;
    }

    public void setUssdPath(String ussdPath) {
        this.ussdPath = ussdPath;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }
}
