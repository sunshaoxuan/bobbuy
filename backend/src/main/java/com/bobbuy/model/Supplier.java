package com.bobbuy.model;

import java.util.HashMap;
import java.util.Map;

public class Supplier {
    private String id;

    @JsonbColumn
    private Map<String, String> name = new HashMap<>();

    @JsonbColumn
    private Map<String, String> description = new HashMap<>();

    private String contactInfo;

    public Supplier() {
    }

    public Supplier(String id, Map<String, String> name, Map<String, String> description, String contactInfo) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contactInfo = contactInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public Map<String, String> getDescription() {
        return description;
    }

    public void setDescription(Map<String, String> description) {
        this.description = description;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}
