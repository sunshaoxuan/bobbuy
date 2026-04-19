package com.bobbuy.api;

import java.time.LocalDateTime;

public record WalletSummaryResponse(
    String partnerId,
    Double balance,
    String currency,
    LocalDateTime updatedAt
) {}
