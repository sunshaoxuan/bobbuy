package com.bobbuy.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单头模型 - 代表一次业务层面的购买事件
 * 对齐设计文档: ARCH-11
 */
public class OrderHeader {
    private Long id; // 物理主键
    private String businessId; // 业务幂等标识 (Event ID)
    private Long customerId; // 客户 ID
    private Long tripId; // 行程 ID
    private OrderStatus status; // 订单状态
    private LocalDateTime statusUpdatedAt;
    private PaymentMethod paymentMethod; // 支付方式
    private PaymentStatus paymentStatus; // 支付状态
    private double totalAmount; // 冗余汇总金额
    private List<OrderLine> lines = new ArrayList<>(); // 嵌套行条目

    public OrderHeader() {
    }

    public OrderHeader(String businessId, Long customerId, Long tripId) {
        this.businessId = businessId;
        this.customerId = customerId;
        this.tripId = tripId;
        this.status = OrderStatus.NEW;
        this.statusUpdatedAt = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.UNPAID;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
    }

    public void addLine(OrderLine line) {
        this.lines.add(line);
        line.setHeaderId(this.id);
    }
}
