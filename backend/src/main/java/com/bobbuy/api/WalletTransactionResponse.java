package com.bobbuy.api;

import java.time.LocalDateTime;

public record WalletTransactionResponse(
    Long id,
    String partnerId,
    Double amount,
    String type,
    Long tripId,
    LocalDateTime createdAt
) {}
