package com.bobbuy.repository;

import com.bobbuy.model.MerchantSku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantSkuRepository extends JpaRepository<MerchantSku, String> {
    void deleteByProductId(String productId);
}
