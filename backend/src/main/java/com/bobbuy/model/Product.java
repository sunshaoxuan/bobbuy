package com.bobbuy.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Product {
    private String id;

    @JsonbColumn
    private Map<String, String> name = new HashMap<>();

    @JsonbColumn
    private Map<String, String> description = new HashMap<>();

    private String brand;
    private double basePrice;

    @JsonbColumn
    private List<MediaGalleryItem> mediaGallery = new ArrayList<>();

    private StorageCondition storageCondition;
    private OrderMethod orderMethod;
    private String categoryId;

    public Product() {
    }

    public Product(String id,
                   Map<String, String> name,
                   Map<String, String> description,
                   String brand,
                   double basePrice,
                   List<MediaGalleryItem> mediaGallery,
                   StorageCondition storageCondition,
                   OrderMethod orderMethod,
                   String categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.basePrice = basePrice;
        this.mediaGallery = mediaGallery;
        this.storageCondition = storageCondition;
        this.orderMethod = orderMethod;
        this.categoryId = categoryId;
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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public List<MediaGalleryItem> getMediaGallery() {
        return mediaGallery;
    }

    public void setMediaGallery(List<MediaGalleryItem> mediaGallery) {
        this.mediaGallery = mediaGallery;
    }

    public StorageCondition getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(StorageCondition storageCondition) {
        this.storageCondition = storageCondition;
    }

    public OrderMethod getOrderMethod() {
        return orderMethod;
    }

    public void setOrderMethod(OrderMethod orderMethod) {
        this.orderMethod = orderMethod;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
