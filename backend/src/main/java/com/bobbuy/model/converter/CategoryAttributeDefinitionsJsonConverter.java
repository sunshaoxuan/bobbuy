package com.bobbuy.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Converter
public class CategoryAttributeDefinitionsJsonConverter implements AttributeConverter<List<Map<String, Object>>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? new ArrayList<>() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize category attributes", e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(normalizeJsonValue(dbData), TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize category attributes", e);
        }
    }

    private String normalizeJsonValue(String dbData) throws JsonProcessingException {
        String normalized = dbData.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
            return OBJECT_MAPPER.readValue(normalized, String.class);
        }
        return normalized;
    }
}
