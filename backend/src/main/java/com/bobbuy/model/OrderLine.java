package com.bobbuy.model;

/**
 * 订单行模型 - 具体的购买条目
 * 对齐设计文档: ARCH-11
 */
public class OrderLine {
    private Long id; // 物理主键
    private Long headerId; // 头关联 ID
    private String skuId; // 商品唯一标识
    private String itemName; // 商品名称 (冗余用于展现)
    private String spec; // 规格区分 (用于隔离权重品)
    private int quantity; // 数量
    private double unitPrice; // 单价

    public OrderLine() {
    }

    public OrderLine(String skuId, String itemName, String spec, int quantity, double unitPrice) {
        this.skuId = skuId;
        this.itemName = itemName;
        this.spec = spec;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHeaderId() {
        return headerId;
    }

    public void setHeaderId(Long headerId) {
        this.headerId = headerId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 判断两行是否符合业务合并准则
     * 准则: SKU 相同 且 Spec 相同
     */
    public boolean canMergeWith(OrderLine other) {
        if (other == null)
            return false;
        boolean skuMatch = this.skuId != null && this.skuId.equals(other.skuId);
        boolean specMatch = (this.spec == null && other.spec == null) ||
                (this.spec != null && this.spec.equals(other.spec));
        return skuMatch && specMatch;
    }
}
