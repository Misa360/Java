package com.codeanalyzer.model;

public class User {
    private String handle;
    private String platform;
    private String lastCrawledAt;

    public User(String handle, String platform, String lastCrawledAt) {
        this.handle = handle;
        this.platform = platform;
        this.lastCrawledAt = lastCrawledAt;
    }

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(String lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }
}
