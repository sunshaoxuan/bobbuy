package com.bobbuy.model;

import java.time.LocalDateTime;

/**
 * Represents a specific price tier for a product.
 * Supports multi-tier pricing strategies (e.g., Member, Agent, Bulk).
 */
public record PriceTier(
    String tierName,   // e.g., "Member", "Agent Cost", "Retail"
    double price,
    String currency,   // e.g., "JPY", "CNY"
    boolean agentOnly, // If true, hidden from customers
    String note,       // e.g., "Valid until 2026-05-01"
    LocalDateTime updatedAt
) {
    public PriceTier(String tierName, double price, String currency, boolean agentOnly) {
        this(tierName, price, currency, agentOnly, null, LocalDateTime.now());
    }
}
