package com.bobbuy.model.converter;

import com.bobbuy.model.MediaGalleryItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class MediaGalleryJsonConverter implements AttributeConverter<List<MediaGalleryItem>, String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<MediaGalleryItem>> TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<MediaGalleryItem> attribute) {
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute == null ? new ArrayList<>() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize media gallery", e);
        }
    }

    @Override
    public List<MediaGalleryItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize media gallery", e);
        }
    }
}
