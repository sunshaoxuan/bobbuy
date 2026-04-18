package com.bobbuy.model.converter;

import com.bobbuy.model.PriceTier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class PriceTierListJsonConverter implements AttributeConverter<List<PriceTier>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // Support LocalDateTime
    
    private static final TypeReference<List<PriceTier>> TYPE_REFERENCE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<PriceTier> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? new ArrayList<>() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize price tiers", e);
        }
    }

    @Override
    public List<PriceTier> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(normalizeJsonValue(dbData), TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize price tiers", e);
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
