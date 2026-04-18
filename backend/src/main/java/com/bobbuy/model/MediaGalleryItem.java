package com.bobbuy.model;

import java.util.HashMap;
import java.util.Map;

public class MediaGalleryItem {
    private String url;
    private MediaType type;
    private boolean visible = true;

    @JsonbColumn
    private Map<String, String> title = new HashMap<>();

    public MediaGalleryItem() {
    }

    public MediaGalleryItem(String url, MediaType type, Map<String, String> title) {
        this.url = url;
        this.type = type;
        this.title = title;
    }

    public MediaGalleryItem(String url, MediaType type, Map<String, String> title, boolean visible) {
        this.url = url;
        this.type = type;
        this.title = title;
        this.visible = visible;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }
}
