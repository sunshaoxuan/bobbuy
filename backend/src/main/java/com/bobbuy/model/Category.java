package com.bobbuy.model;

import com.bobbuy.model.converter.CategoryAttributeDefinitionsJsonConverter;
import com.bobbuy.model.converter.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "bb_category")
public class Category {
    @Id
    private String id;

    @JsonbColumn
    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> name = new HashMap<>();

    @JsonbColumn
    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> description = new HashMap<>();

    @JsonbColumn
    @Convert(converter = CategoryAttributeDefinitionsJsonConverter.class)
    @Column(columnDefinition = "jsonb")
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
