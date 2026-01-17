package com.bobbuy.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderItem {
    private Long id;

    @NotBlank(message = "{validation.order.item_name.required}")
    private String itemName;

    @Min(value = 1, message = "{validation.order.quantity.min}")
    private int quantity;

    @Min(value = 0, message = "{validation.order.unit_price.min}")
    private double unitPrice;

    @NotNull
    private boolean variable; // 是否为计重/非标品

    public OrderItem() {
    }

    public OrderItem(Long id, String itemName, int quantity, double unitPrice, boolean variable) {
        this.id = id;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.variable = variable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public boolean isVariable() {
        return variable;
    }

    public void setVariable(boolean variable) {
        this.variable = variable;
    }
}
