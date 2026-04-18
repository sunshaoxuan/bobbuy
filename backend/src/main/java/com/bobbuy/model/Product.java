package com.bobbuy.model;

import com.bobbuy.model.converter.MediaGalleryJsonConverter;
import com.bobbuy.model.converter.PriceTierListJsonConverter;
import com.bobbuy.model.converter.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "bb_product")
public class Product {
    @Id
    private String id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> name = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> description = new HashMap<>();

    private String brand;
    private double basePrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<MediaGalleryItem> mediaGallery = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private StorageCondition storageCondition;
    @Enumerated(EnumType.STRING)
    private OrderMethod orderMethod;
    private String categoryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "merchant_skus", columnDefinition = "jsonb")
    private Map<String, String> merchantSkus;

    private String itemNumber; // For AI matching and shelf identification

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "price_tiers", columnDefinition = "jsonb")
    private List<PriceTier> priceTiers = new ArrayList<>();

    public Product() {
        this.merchantSkus = new HashMap<>();
    }

    public Product(String id,
                   Map<String, String> name,
                   Map<String, String> description,
                   String brand,
                   double basePrice,
                    List<MediaGalleryItem> mediaGallery,
                    StorageCondition storageCondition,
                    OrderMethod orderMethod,
                    String categoryId,
                    Map<String, String> merchantSkus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.basePrice = basePrice;
        this.mediaGallery = mediaGallery;
        this.storageCondition = storageCondition;
        this.orderMethod = orderMethod;
        this.categoryId = categoryId;
        this.merchantSkus = merchantSkus == null ? new HashMap<>() : merchantSkus;
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

    public Map<String, String> getMerchantSkus() {
        return merchantSkus;
    }

    public void setMerchantSkus(Map<String, String> merchantSkus) {
        this.merchantSkus = merchantSkus;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public List<PriceTier> getPriceTiers() {
        return priceTiers;
    }

    public void setPriceTiers(List<PriceTier> priceTiers) {
        this.priceTiers = priceTiers;
    }
}
