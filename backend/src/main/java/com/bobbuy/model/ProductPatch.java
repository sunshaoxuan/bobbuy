package com.bobbuy.model;

import java.util.List;
import java.util.Map;

public class ProductPatch {
    private Map<String, String> name;
    private Map<String, String> description;
    private String brand;
    private Double basePrice;
    private List<MediaGalleryItem> mediaGallery;
    private StorageCondition storageCondition;
    private OrderMethod orderMethod;
    private String categoryId;
    private Map<String, String> merchantSkus;

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

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
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

    public Map<String, String> getMerchantSkus() {
        return merchantSkus;
    }

    public void setMerchantSkus(Map<String, String> merchantSkus) {
        this.merchantSkus = merchantSkus;
    }
}
