package com.bobbuy.repository;

import com.bobbuy.model.PartnerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PartnerWalletRepository extends JpaRepository<PartnerWallet, Long> {
  Optional<PartnerWallet> findByPartnerId(String partnerId);
}
