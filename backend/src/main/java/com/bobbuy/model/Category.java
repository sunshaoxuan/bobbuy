package com.bobbuy.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Category {
    private String id;

    @JsonbColumn
    private Map<String, String> name = new HashMap<>();

    @JsonbColumn
    private Map<String, String> description = new HashMap<>();

    @JsonbColumn
    private List<Map<String, Object>> attributeDefinitions = new ArrayList<>();

    public Category() {
    }

    public Category(String id,
                    Map<String, String> name,
                    Map<String, String> description,
                    List<Map<String, Object>> attributeDefinitions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attributeDefinitions = attributeDefinitions;
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

    public List<Map<String, Object>> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(List<Map<String, Object>> attributeDefinitions) {
        this.attributeDefinitions = attributeDefinitions;
    }
}
