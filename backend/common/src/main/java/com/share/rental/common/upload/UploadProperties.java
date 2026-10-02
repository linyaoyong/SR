package com.share.rental.common.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String root = "../uploads";
    private long maxBytes = 1048576L;
    private List<String> allowedBuckets = List.of("avatars", "items", "messages", "reviews", "audit");
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");

    public UploadProperties() {
    }

    public static UploadProperties defaults() {
        return new UploadProperties();
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public List<String> getAllowedBuckets() {
        return allowedBuckets;
    }

    public void setAllowedBuckets(List<String> allowedBuckets) {
        this.allowedBuckets = allowedBuckets;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
