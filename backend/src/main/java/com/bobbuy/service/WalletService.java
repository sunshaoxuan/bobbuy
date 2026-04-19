package com.bobbuy.service;

import com.bobbuy.model.PartnerWallet;
import com.bobbuy.model.WalletTransaction;
import com.bobbuy.repository.PartnerWalletRepository;
import com.bobbuy.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {
  private final PartnerWalletRepository partnerWalletRepository;
  private final WalletTransactionRepository walletTransactionRepository;

  public WalletService(PartnerWalletRepository partnerWalletRepository,
                       WalletTransactionRepository walletTransactionRepository) {
    this.partnerWalletRepository = partnerWalletRepository;
    this.walletTransactionRepository = walletTransactionRepository;
  }

  @Transactional
  public void payout(String partnerId, Double amount, Long tripId) {
    if (amount <= 0) return;

    PartnerWallet wallet = partnerWalletRepository.findByPartnerId(partnerId)
        .orElseGet(() -> new PartnerWallet(partnerId, 0D, "CNY", LocalDateTime.now()));

    wallet.setBalance(wallet.getBalance() + amount);
    wallet.setUpdatedAt(LocalDateTime.now());
    partnerWalletRepository.save(wallet);

    WalletTransaction transaction = new WalletTransaction(
        partnerId,
        amount,
        "TRIP_PAYOUT",
        tripId,
        LocalDateTime.now());
    walletTransactionRepository.save(transaction);
  }

  @Transactional(readOnly = true)
  public PartnerWallet getWallet(String partnerId) {
    return partnerWalletRepository.findByPartnerId(partnerId)
        .orElseGet(() -> new PartnerWallet(partnerId, 0D, "CNY", LocalDateTime.now()));
  }

  @Transactional(readOnly = true)
  public List<WalletTransaction> getTransactions(String partnerId) {
    return walletTransactionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
  }
}
