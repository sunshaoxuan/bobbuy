package com.bobbuy.model;

public class MerchantSku {
    private String id;
    private String productId;
    private String merchantId;
    private String skuCode;
    private double lastPrice;
    private StockStatus stockStatus;

    public MerchantSku() {
    }

    public MerchantSku(String id, String productId, String merchantId, String skuCode, double lastPrice, StockStatus stockStatus) {
        this.id = id;
        this.productId = productId;
        this.merchantId = merchantId;
        this.skuCode = skuCode;
        this.lastPrice = lastPrice;
        this.stockStatus = stockStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public StockStatus getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(StockStatus stockStatus) {
        this.stockStatus = stockStatus;
    }
}
