package com.bobbuy.api;

import com.bobbuy.model.Product;

import java.util.List;

public class MobileProductResponse {
    private Product product;
    private String displayName;
    private String displayDescription;
    private int reconciledQuantity;
    private Long reconciledTripId;
    private List<String> allocatedBusinessIds;

    public MobileProductResponse() {
    }

    public MobileProductResponse(Product product, String displayName, String displayDescription) {
        this.product = product;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
    }

    public MobileProductResponse(Product product,
                                 String displayName,
                                 String displayDescription,
                                 int reconciledQuantity,
                                 Long reconciledTripId,
                                 List<String> allocatedBusinessIds) {
        this.product = product;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
        this.reconciledQuantity = reconciledQuantity;
        this.reconciledTripId = reconciledTripId;
        this.allocatedBusinessIds = allocatedBusinessIds;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayDescription() {
        return displayDescription;
    }

    public void setDisplayDescription(String displayDescription) {
        this.displayDescription = displayDescription;
    }

    public int getReconciledQuantity() {
        return reconciledQuantity;
    }

    public void setReconciledQuantity(int reconciledQuantity) {
        this.reconciledQuantity = reconciledQuantity;
    }

    public Long getReconciledTripId() {
        return reconciledTripId;
    }

    public void setReconciledTripId(Long reconciledTripId) {
        this.reconciledTripId = reconciledTripId;
    }

    public List<String> getAllocatedBusinessIds() {
        return allocatedBusinessIds;
    }

    public void setAllocatedBusinessIds(List<String> allocatedBusinessIds) {
        this.allocatedBusinessIds = allocatedBusinessIds;
    }
}
