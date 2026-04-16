package com.bobbuy.api;

import com.bobbuy.model.Product;

public class MobileProductResponse {
    private Product product;
    private String displayName;
    private String displayDescription;

    public MobileProductResponse() {
    }

    public MobileProductResponse(Product product, String displayName, String displayDescription) {
        this.product = product;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
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
}
