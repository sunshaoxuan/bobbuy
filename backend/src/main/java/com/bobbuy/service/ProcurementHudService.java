package com.bobbuy.service;

import com.bobbuy.api.ProcurementDeficitItemResponse;
import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.api.CustomerBalanceLedgerResponse;
import com.bobbuy.api.FinancialAuditLogResponse;
import com.bobbuy.api.LogisticsTrackingRequest;
import com.bobbuy.api.LogisticsTrackingResponse;
import com.bobbuy.api.LedgerConfirmationRequest;
import com.bobbuy.api.PartnerProfitShareResponse;
import com.bobbuy.api.ProcurementReceiptResponse;
import com.bobbuy.api.ProcurementReceiptSaveRequest;
import com.bobbuy.api.ProcurementReceiptUploadRequest;
import com.bobbuy.api.ProfitSharingConfigRequest;
import com.bobbuy.api.ProfitSharingConfigResponse;
import com.bobbuy.api.ReceiptPreviewResponse;
import com.bobbuy.api.TripExpenseRequest;
import com.bobbuy.api.TripExpenseResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.PaymentStatus;
import com.bobbuy.model.ProcurementReceipt;
import com.bobbuy.model.Product;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripExpense;
import com.bobbuy.model.TripLogisticsTracking;
import com.bobbuy.model.TripStatus;
import com.bobbuy.model.TripProfitShareConfig;
import com.bobbuy.model.LogisticsChannel;
import com.bobbuy.model.LogisticsProvider;
import com.bobbuy.model.LogisticsStatus;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.repository.ProcurementReceiptRepository;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.TripRepository;
import com.bobbuy.repository.TripExpenseRepository;
import com.bobbuy.repository.TripLogisticsTrackingRepository;
import com.bobbuy.repository.TripProfitShareConfigRepository;
import com.bobbuy.api.WalletSummaryResponse;
import com.bobbuy.api.WalletTransactionResponse;
import com.bobbuy.security.CustomerIdentityResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ProcurementHudService {
  private static final String UNCATEGORIZED = "UNCATEGORIZED";
  private static final String OCR_STATUS_NOT_UPLOADED = "NOT_UPLOADED";
  private static final String OCR_STATUS_PENDING = "PENDING";
  private static final String RECEIPT_STATUS_READY = "READY_FOR_REVIEW";
  private static final String RECEIPT_STATUS_RECONCILED = "RECONCILED";
  private static final double DEFAULT_PURCHASER_RATIO = 70D;
  private static final double DEFAULT_PROMOTER_RATIO = 30D;
  private static final double DEFAULT_FX_RATE = 1D;
  private static final Logger log = LoggerFactory.getLogger(ProcurementHudService.class);

  private final TripRepository tripRepository;
  private final OrderHeaderRepository orderHeaderRepository;
  private final ProductRepository productRepository;
  private final TripExpenseRepository tripExpenseRepository;
  private final ProcurementReceiptRepository procurementReceiptRepository;
  private final TripProfitShareConfigRepository tripProfitShareConfigRepository;
  private final TripLogisticsTrackingRepository tripLogisticsTrackingRepository;
  private final FinancialAuditTrailService financialAuditTrailService;
  private final FxRateService fxRateService;
  private final ImageStorageService imageStorageService;
  private final ObjectMapper objectMapper;
  private final WalletService walletService;
  private final CustomerIdentityResolver customerIdentityResolver;
  private final double referenceFxRate;

  public ProcurementHudService(TripRepository tripRepository,
                               OrderHeaderRepository orderHeaderRepository,
                               ProductRepository productRepository,
                               TripExpenseRepository tripExpenseRepository,
                               ProcurementReceiptRepository procurementReceiptRepository,
                               TripProfitShareConfigRepository tripProfitShareConfigRepository,
                               TripLogisticsTrackingRepository tripLogisticsTrackingRepository,
                               FinancialAuditTrailService financialAuditTrailService,
                               FxRateService fxRateService,
                               ImageStorageService imageStorageService,
                               ObjectMapper objectMapper,
                               WalletService walletService,
                               CustomerIdentityResolver customerIdentityResolver,
                               @Value("${bobbuy.fx.reference-rate:1.0}") double referenceFxRate) {
    this.tripRepository = tripRepository;
    this.orderHeaderRepository = orderHeaderRepository;
    this.productRepository = productRepository;
    this.tripExpenseRepository = tripExpenseRepository;
    this.procurementReceiptRepository = procurementReceiptRepository;
    this.tripProfitShareConfigRepository = tripProfitShareConfigRepository;
    this.tripLogisticsTrackingRepository = tripLogisticsTrackingRepository;
    this.financialAuditTrailService = financialAuditTrailService;
    this.fxRateService = fxRateService;
    this.imageStorageService = imageStorageService;
    this.objectMapper = objectMapper;
    this.walletService = walletService;
    this.customerIdentityResolver = customerIdentityResolver;
    this.referenceFxRate = referenceFxRate;
  }

  @Transactional(readOnly = true)
  public ProcurementHudResponse getHudStats(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    List<OrderHeader> orders = orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId);

    double actualCostBase = 0D;
    double expectedRevenueBase = 0D;
    double totalWeight = 0D;
    double totalVolume = 0D;

    Map<String, Integer> categoryExpected = new LinkedHashMap<>();
    Map<String, Integer> categoryPurchased = new LinkedHashMap<>();

    for (OrderHeader order : orders) {
      for (OrderLine line : safeLines(order)) {
        int required = Math.max(line.getQuantity(), 0);
        int purchased = Math.min(Math.max(line.getPurchasedQuantity(), 0), required);

        expectedRevenueBase += required * line.getUnitPrice();
        actualCostBase += purchased * line.getUnitPrice();

        Optional<Product> product = resolveProduct(line.getSkuId());
        totalWeight += purchased * sanitizePhysicalMetric(product.map(Product::getWeight).orElse(null));
        totalVolume += purchased * sanitizePhysicalMetric(product.map(Product::getVolume).orElse(null));

        String categoryId = product
            .map(Product::getCategoryId)
            .filter(value -> value != null && !value.isBlank())
            .orElse(UNCATEGORIZED);
        categoryExpected.merge(categoryId, required, Integer::sum);
        categoryPurchased.merge(categoryId, purchased, Integer::sum);
      }
    }

    trip.setCurrentWeight(totalWeight);
    trip.setCurrentVolume(totalVolume);
    double safeReferenceRate = safeRate(referenceFxRate, "bobbuy.fx.reference-rate");
    double safeCurrentRate = safeRate(fxRateService.resolveCurrentRate(), "bobbuy.fx.current-rate.dynamic");
    double totalTripExpenses = Math.max(tripExpenseRepository.sumCostByTripId(tripId), 0D);
    double purchasedAmount = actualCostBase * safeCurrentRate;
    double estimatedProfit = (expectedRevenueBase * safeReferenceRate) - purchasedAmount - totalTripExpenses;
    TripProfitShareConfig config = getOrCreateProfitShareConfig(tripId);
    List<PartnerProfitShareResponse> partnerShares = calculatePartnerShares(estimatedProfit, config);

    return new ProcurementHudResponse(
        tripId,
        round2(estimatedProfit),
        round2(purchasedAmount),
        round2(safeCurrentRate),
        round2(safeReferenceRate),
        round2(totalTripExpenses),
        trip.getCurrentWeight(),
        trip.getCurrentVolume(),
        calculateCategoryCompletion(categoryExpected, categoryPurchased),
        partnerShares);
  }

  @Transactional(readOnly = true)
  public List<TripExpenseResponse> getTripExpenses(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return tripExpenseRepository.findByTripIdOrderByCreatedAtDescIdDesc(tripId).stream()
        .map(item -> new TripExpenseResponse(
            item.getId(),
            item.getTripId(),
            round2(item.getCost()),
            item.getCategory(),
            item.getReceiptThumbnailUrl(),
            item.getOcrStatus(),
            item.getCreatedAt()))
        .toList();
  }

  @Transactional
  public TripExpenseResponse createTripExpense(Long tripId, TripExpenseRequest request) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (request == null || request.getCost() <= 0 || request.getCategory() == null || request.getCategory().isBlank()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.expense.invalid");
    }
    ImageStorageService.UploadResult receiptUpload = imageStorageService.saveBase64ToObject(request.getReceiptImageBase64());
    String receiptObjectKey = receiptUpload == null ? null : receiptUpload.objectKey();
    String receiptThumbnailUrl = receiptUpload == null ? null : receiptUpload.publicUrl();
    String ocrStatus = receiptObjectKey == null ? OCR_STATUS_NOT_UPLOADED : OCR_STATUS_PENDING;

    TripExpense expense = new TripExpense(
        tripId,
        request.getCost(),
        request.getCategory().trim(),
        receiptObjectKey,
        receiptThumbnailUrl,
        ocrStatus,
        LocalDateTime.now());
    TripExpense saved = tripExpenseRepository.save(expense);
    Map<String, Object> original = new HashMap<>();
    original.put("cost", 0D);
    original.put("category", "");
    Map<String, Object> modified = new HashMap<>();
    modified.put("cost", round2(saved.getCost()));
    modified.put("category", saved.getCategory());
    modified.put("receiptObjectKey", saved.getReceiptObjectKey());
    modified.put("ocrStatus", saved.getOcrStatus());
    financialAuditTrailService.logExpenseCreate(
        tripId,
        serializeAuditPayload(original),
        serializeAuditPayload(modified));
    return new TripExpenseResponse(
        saved.getId(),
        saved.getTripId(),
        round2(saved.getCost()),
        saved.getCategory(),
        saved.getReceiptThumbnailUrl(),
        saved.getOcrStatus(),
        saved.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public List<ProcurementDeficitItemResponse> getDeficitItems(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));

    Map<String, DeficitAggregate> deficitBySku = new LinkedHashMap<>();
    for (OrderHeader order : orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId)) {
      for (OrderLine line : safeLines(order)) {
        int required = Math.max(line.getQuantity(), 0);
        int purchased = Math.min(Math.max(line.getPurchasedQuantity(), 0), required);
        int deficit = Math.max(required - purchased, 0);
        if (deficit <= 0) {
          continue;
        }
        String sku = line.getSkuId();
        DeficitAggregate aggregate = deficitBySku.computeIfAbsent(
            sku,
            key -> new DeficitAggregate(key, line.getItemName(), order.getCreatedAt()));
        aggregate.required += required;
        aggregate.purchased += purchased;
        aggregate.deficit += deficit;
        if (order.getCreatedAt() != null && (aggregate.firstSeenAt == null || order.getCreatedAt().isBefore(aggregate.firstSeenAt))) {
          aggregate.firstSeenAt = order.getCreatedAt();
        }
      }
    }

        return deficitBySku.values().stream()
                .sorted(Comparator
                        .comparingInt(DeficitAggregate::priorityRank)
                        .thenComparing((DeficitAggregate item) -> item.deficit, Comparator.reverseOrder())
                        .thenComparing(item -> item.firstSeenAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> {
                    Optional<Product> product = resolveProduct(item.skuId);
                    return new ProcurementDeficitItemResponse(
                            item.skuId,
                            item.itemName,
                            item.deficit,
                            round2(item.completionPercent()),
                            item.priority(),
                            product.map(Product::isTemporary).orElse(false),
                            product.map(p -> p.getVisibilityStatus() != null ? p.getVisibilityStatus().name() : "PUBLIC").orElse("PUBLIC")
                    );
                })
                .collect(Collectors.toList());
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public int reconcileInventory(String productId, int quantity) {
    return reconcileInventoryWithDetails(productId, quantity).reconciledQuantity();
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public ReconcileInventoryResult reconcileInventoryWithDetails(String productId, int quantity) {
    if (productId == null || productId.isBlank() || quantity <= 0) {
      return ReconcileInventoryResult.empty();
    }

    List<OrderHeader> draftOrders = orderHeaderRepository.findByStatusForUpdate(OrderStatus.NEW);
    List<OrderHeader> candidateOrders = draftOrders.stream()
        .filter(order -> safeLines(order).stream().anyMatch(line -> Objects.equals(line.getSkuId(), productId)))
        .sorted(Comparator
            .comparing(OrderHeader::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OrderHeader::getId))
        .toList();

    if (candidateOrders.isEmpty()) {
      return ReconcileInventoryResult.empty();
    }

    Long targetTripId = candidateOrders.get(0).getTripId();
    ensureTripMutable(targetTripId);
    int remaining = quantity;
    int reconciled = 0;
    List<OrderHeader> changedOrders = new ArrayList<>();
    LinkedHashSet<String> allocatedBusinessIds = new LinkedHashSet<>();

    for (OrderHeader order : candidateOrders) {
      if (!Objects.equals(order.getTripId(), targetTripId)) {
        continue;
      }
      boolean changed = false;
      for (OrderLine line : safeLines(order)) {
        if (!Objects.equals(line.getSkuId(), productId)) {
          continue;
        }
        int required = Math.max(line.getQuantity(), 0);
        int purchased = Math.min(Math.max(line.getPurchasedQuantity(), 0), required);
        int deficit = Math.max(required - purchased, 0);
        if (deficit <= 0) {
          continue;
        }
        int fill = Math.min(deficit, remaining);
        line.setPurchasedQuantity(purchased + fill);
        remaining -= fill;
        reconciled += fill;
        if (fill > 0 && order.getBusinessId() != null && !order.getBusinessId().isBlank()) {
          allocatedBusinessIds.add(order.getBusinessId());
        }
        changed = true;
        if (remaining == 0) {
          break;
        }
      }
      if (changed) {
        changedOrders.add(order);
      }
      if (remaining == 0) {
        break;
      }
    }

    if (!changedOrders.isEmpty()) {
      orderHeaderRepository.saveAll(changedOrders);
    }
    return new ReconcileInventoryResult(reconciled, targetTripId, new ArrayList<>(allocatedBusinessIds));
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public int manualReconcile(Long tripId, String skuId, String fromBusinessId, String toBusinessId, int quantity) {
    if (tripId == null || skuId == null || skuId.isBlank() || fromBusinessId == null || fromBusinessId.isBlank()
        || toBusinessId == null || toBusinessId.isBlank() || quantity <= 0 || Objects.equals(fromBusinessId, toBusinessId)) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.reconcile.invalid");
    }

    ensureTripMutable(tripId);
    List<OrderHeader> orders = orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId);
    OrderHeader fromOrder = orders.stream()
        .filter(order -> Objects.equals(order.getBusinessId(), fromBusinessId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.reconcile.source_not_found"));
    OrderHeader toOrder = orders.stream()
        .filter(order -> Objects.equals(order.getBusinessId(), toBusinessId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.reconcile.target_not_found"));

    OrderLine sourceLine = safeLines(fromOrder).stream()
        .filter(line -> Objects.equals(line.getSkuId(), skuId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.reconcile.source_line_not_found"));
    OrderLine targetLine = safeLines(toOrder).stream()
        .filter(line -> Objects.equals(line.getSkuId(), skuId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.reconcile.target_line_not_found"));

    int sourcePurchased = Math.max(sourceLine.getPurchasedQuantity(), 0);
    int targetRequired = Math.max(targetLine.getQuantity(), 0);
    int targetPurchased = Math.min(Math.max(targetLine.getPurchasedQuantity(), 0), targetRequired);
    int targetDeficit = Math.max(targetRequired - targetPurchased, 0);
    int transferred = Math.min(quantity, Math.min(sourcePurchased, targetDeficit));
    if (transferred <= 0) {
      return 0;
    }

    sourceLine.setPurchasedQuantity(sourcePurchased - transferred);
    targetLine.setPurchasedQuantity(targetPurchased + transferred);
    orderHeaderRepository.saveAll(List.of(fromOrder, toOrder));
    Map<String, Object> original = new HashMap<>();
    original.put("skuId", skuId);
    original.put("fromBusinessId", fromBusinessId);
    original.put("toBusinessId", toBusinessId);
    original.put("fromPurchased", sourcePurchased);
    original.put("toPurchased", targetPurchased);

    Map<String, Object> modified = new HashMap<>();
    modified.put("skuId", skuId);
    modified.put("fromBusinessId", fromBusinessId);
    modified.put("toBusinessId", toBusinessId);
    modified.put("fromPurchased", sourceLine.getPurchasedQuantity());
    modified.put("toPurchased", targetLine.getPurchasedQuantity());
    modified.put("transferred", transferred);

    financialAuditTrailService.logManualReconcile(
        tripId,
        serializeAuditPayload(original),
        serializeAuditPayload(modified));
    return transferred;
  }

  @Transactional(readOnly = true)
  public List<FinancialAuditLogResponse> getFinancialAuditLogs(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return financialAuditTrailService.listByTripId(tripId);
  }

  @Transactional(readOnly = true)
  public ProfitSharingConfigResponse getProfitSharingConfig(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    TripProfitShareConfig config = getOrCreateProfitShareConfig(tripId);
    double netProfit = getHudStats(tripId).getTotalEstimatedProfit();
    return new ProfitSharingConfigResponse(
        tripId,
        round2(config.getPurchaserRatioPercent()),
        round2(config.getPromoterRatioPercent()),
        calculatePartnerShares(netProfit, config));
  }

  @Transactional
  public ProfitSharingConfigResponse updateProfitSharingConfig(Long tripId, ProfitSharingConfigRequest request) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (request == null
        || request.getPurchaserRatioPercent() < 0
        || request.getPromoterRatioPercent() < 0
        || Math.abs((request.getPurchaserRatioPercent() + request.getPromoterRatioPercent()) - 100D) > 0.001D) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.profit_share.invalid");
    }
    TripProfitShareConfig config = getOrCreateProfitShareConfig(tripId);
    Map<String, Object> original = new HashMap<>();
    original.put("purchaserRatioPercent", round2(config.getPurchaserRatioPercent()));
    original.put("promoterRatioPercent", round2(config.getPromoterRatioPercent()));

    config.setPurchaserRatioPercent(round2(request.getPurchaserRatioPercent()));
    config.setPromoterRatioPercent(round2(request.getPromoterRatioPercent()));
    config.setUpdatedAt(LocalDateTime.now());
    tripProfitShareConfigRepository.save(config);

    Map<String, Object> modified = new HashMap<>();
    modified.put("purchaserRatioPercent", round2(config.getPurchaserRatioPercent()));
    modified.put("promoterRatioPercent", round2(config.getPromoterRatioPercent()));
    financialAuditTrailService.logProfitShareRatioUpdate(
        tripId,
        serializeAuditPayload(original),
        serializeAuditPayload(modified));
    double netProfit = getHudStats(tripId).getTotalEstimatedProfit();
    return new ProfitSharingConfigResponse(
        tripId,
        round2(config.getPurchaserRatioPercent()),
        round2(config.getPromoterRatioPercent()),
        calculatePartnerShares(netProfit, config));
  }

  @Transactional(readOnly = true)
  public List<LogisticsTrackingResponse> getTripLogisticsTrackings(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return tripLogisticsTrackingRepository.findByTripIdOrderByUpdatedAtDescIdDesc(tripId).stream()
        .map(this::toLogisticsResponse)
        .toList();
  }

  @Transactional
  public LogisticsTrackingResponse createTripLogisticsTracking(Long tripId, LogisticsTrackingRequest request) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (request == null || request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.logistics.invalid");
    }
    LocalDateTime now = LocalDateTime.now();
    TripLogisticsTracking tracking = new TripLogisticsTracking(
        tripId,
        request.getTrackingNumber().trim(),
        parseChannel(request.getChannel()),
        parseProvider(request.getProvider()),
        LogisticsStatus.PENDING,
        "Tracking created",
        false,
        now,
        now,
        now);
    return toLogisticsResponse(tripLogisticsTrackingRepository.save(tracking));
  }

  @Transactional
  public LogisticsTrackingResponse refreshTripLogisticsTracking(Long tripId, Long trackingId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    TripLogisticsTracking tracking = tripLogisticsTrackingRepository.findById(trackingId)
        .filter(item -> Objects.equals(item.getTripId(), tripId))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.logistics.not_found"));
    LogisticsStatus status = resolveLogisticsStatus(tracking);
    tracking.setStatus(status);
    tracking.setLastMessage(resolveLogisticsMessage(tracking, status));
    tracking.setLastCheckedAt(LocalDateTime.now());
    tracking.setUpdatedAt(LocalDateTime.now());
    if (status == LogisticsStatus.DELIVERED && !tracking.isSettlementReminderTriggered()) {
      tracking.setSettlementReminderTriggered(true);
      Map<String, Object> original = new HashMap<>();
      original.put("trackingId", tracking.getId());
      original.put("trackingNumber", tracking.getTrackingNumber());
      original.put("settlementReminderTriggered", false);
      Map<String, Object> modified = new HashMap<>();
      modified.put("trackingId", tracking.getId());
      modified.put("trackingNumber", tracking.getTrackingNumber());
      modified.put("settlementReminderTriggered", true);
      modified.put("status", LogisticsStatus.DELIVERED.name());
      financialAuditTrailService.logSettlementReminderTriggered(
          tripId,
          serializeAuditPayload(original),
          serializeAuditPayload(modified));
    }
    return toLogisticsResponse(tripLogisticsTrackingRepository.save(tracking));
  }

  @Transactional(readOnly = true)
  public ReceiptPreviewResponse getExpenseReceiptPreview(Long tripId, Long expenseId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    TripExpense expense = tripExpenseRepository.findById(expenseId)
        .filter(item -> Objects.equals(item.getTripId(), tripId))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.expense.not_found"));
    if (expense.getReceiptObjectKey() == null || expense.getReceiptObjectKey().isBlank()) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.receipt.not_found");
    }
    String previewUrl = imageStorageService.generatePreviewUrl(expense.getReceiptObjectKey());
    if (previewUrl == null || previewUrl.isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "error.procurement.receipt.preview_failed");
    }
    return new ReceiptPreviewResponse(expenseId, previewUrl);
  }

  @Transactional(readOnly = true)
  /**
   * Returns trip ledger entries.
   * AGENT receives full trip ledger; CUSTOMER receives only entries scoped to its injected identity.
   */
  public List<CustomerBalanceLedgerResponse> getCustomerBalanceLedger(Long tripId, Authentication authentication) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    List<CustomerBalanceLedgerResponse> entries = orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId).stream()
        .map(order -> {
          List<CustomerBalanceLedgerResponse.LedgerOrderLineResponse> orderLines = safeLines(order).stream()
              .map(line -> new CustomerBalanceLedgerResponse.LedgerOrderLineResponse(
                  line.getSkuId(),
                  line.getItemName(),
                  Math.max(line.getQuantity(), 0),
                  round2(line.getUnitPrice()),
                  Math.max(line.getPurchasedQuantity(), 0),
                  buildDifferenceNote(line)))
              .toList();
          double totalReceivable = safeLines(order).stream()
              .mapToDouble(line -> {
                int settledQty = Math.max(line.getPurchasedQuantity(), 0) > 0 ? line.getPurchasedQuantity() : line.getQuantity();
                return settledQty * line.getUnitPrice();
              })
              .sum();
          double paidDeposit = order.getPaymentStatus() == PaymentStatus.PAID ? totalReceivable : 0D;
          return new CustomerBalanceLedgerResponse(
              tripId,
              order.getBusinessId(),
              order.getCustomerId(),
              round2(totalReceivable),
              round2(paidDeposit),
              round2(Math.max(totalReceivable - paidDeposit, 0D)),
              resolveSettlementStatus(order),
              trip.isSettlementFrozen(),
              trip.getSettlementFreezeStage(),
              trip.getSettlementFreezeReason(),
              order.getReceiptConfirmedAt(),
              order.getReceiptConfirmedBy(),
              order.getBillingConfirmedAt(),
              order.getBillingConfirmedBy(),
              orderLines);
        })
        .toList();
    if (customerIdentityResolver.isCustomer(authentication)) {
      return filterLedgerForCustomer(entries, authentication);
    }
    return entries;
  }

  private List<CustomerBalanceLedgerResponse> filterLedgerForCustomer(List<CustomerBalanceLedgerResponse> entries,
                                                                      Authentication authentication) {
    Long customerId = customerIdentityResolver.resolveCustomerId(authentication).orElse(null);
    if (customerId == null) {
      return List.of();
    }
    return entries.stream()
        .filter(entry -> Objects.equals(entry.getCustomerId(), customerId))
        .toList();
  }

  @Transactional
  public void finalizeTripSettlement(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (trip.getStatus() == TripStatus.SETTLED) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.settlement.already_settled");
    }

    ProcurementHudResponse stats = getHudStats(tripId);
    double totalEstimatedProfit = stats.getTotalEstimatedProfit();
    TripProfitShareConfig config = getOrCreateProfitShareConfig(tripId);
    List<PartnerProfitShareResponse> shares = calculatePartnerShares(totalEstimatedProfit, config);

    for (PartnerProfitShareResponse share : shares) {
      walletService.payout(share.getPartnerRole(), share.getAmount(), tripId);
    }

    TripStatus oldStatus = trip.getStatus();
    trip.setStatus(TripStatus.SETTLED);
    tripRepository.save(trip);

    Map<String, Object> audit = new HashMap<>();
    audit.put("tripId", tripId);
    audit.put("totalProfit", totalEstimatedProfit);
    audit.put("payouts", shares);
    audit.put("oldStatus", oldStatus);
    audit.put("newStatus", TripStatus.SETTLED);

    financialAuditTrailService.logSettlementReminderTriggered(
        tripId,
        "PROFIT_FINALIZED",
        serializeAuditPayload(audit));
    financialAuditTrailService.logSettlementFreeze(
        tripId,
        "SYSTEM",
        serializeAuditPayload(Map.of("status", oldStatus.name())),
        serializeAuditPayload(Map.of(
            "status", TripStatus.SETTLED.name(),
            "settlementFreezeStage", trip.getSettlementFreezeStage())));
  }

  @Transactional
  public CustomerBalanceLedgerResponse confirmCustomerLedger(Long tripId,
                                                             String businessId,
                                                             LedgerConfirmationRequest request,
                                                             Authentication authentication) {
    if (request == null || request.getAction() == null || request.getAction().isBlank()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.billing.confirm.invalid");
    }
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    OrderHeader order = getTripOrderByBusinessId(tripId, businessId);
    if (customerIdentityResolver.isCustomer(authentication)) {
      Long customerId = customerIdentityResolver.resolveCustomerId(authentication).orElse(null);
      if (!Objects.equals(customerId, order.getCustomerId())) {
        throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found");
      }
    }
    String operatorName = resolveOperatorName(authentication);
    String action = request.getAction().trim().toUpperCase();
    Map<String, Object> original = new LinkedHashMap<>();
    original.put("receiptConfirmedAt", order.getReceiptConfirmedAt());
    original.put("billingConfirmedAt", order.getBillingConfirmedAt());

    if ("RECEIPT".equals(action)) {
      if (order.getReceiptConfirmedAt() != null) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.billing.receipt_already_confirmed");
      }
      LocalDateTime now = LocalDateTime.now();
      order.setReceiptConfirmedAt(now);
      order.setReceiptConfirmedBy(operatorName);
      orderHeaderRepository.save(order);
      financialAuditTrailService.logCustomerReceiptConfirmation(
          tripId,
          operatorName,
          serializeAuditPayload(original),
          serializeAuditPayload(Map.of("receiptConfirmedAt", now, "receiptConfirmedBy", operatorName)));
    } else if ("BILLING".equals(action)) {
      if (order.getReceiptConfirmedAt() == null) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.billing.receipt_required");
      }
      if (order.getBillingConfirmedAt() != null) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.billing.statement_already_confirmed");
      }
      LocalDateTime now = LocalDateTime.now();
      order.setBillingConfirmedAt(now);
      order.setBillingConfirmedBy(operatorName);
      orderHeaderRepository.save(order);
      financialAuditTrailService.logCustomerBillingConfirmation(
          tripId,
          operatorName,
          serializeAuditPayload(original),
          serializeAuditPayload(Map.of("billingConfirmedAt", now, "billingConfirmedBy", operatorName)));
    } else {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.billing.confirm.invalid");
    }
    return buildLedgerEntry(trip, order);
  }

  @Transactional(readOnly = true)
  public List<ProcurementReceiptResponse> getProcurementReceipts(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return procurementReceiptRepository.findByTripIdOrderByUploadedAtDescIdDesc(tripId).stream()
        .map(this::toProcurementReceiptResponse)
        .toList();
  }

  @Transactional
  public List<ProcurementReceiptResponse> uploadProcurementReceipts(Long tripId,
                                                                    ProcurementReceiptUploadRequest request,
                                                                    Authentication authentication) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (request == null || request.getReceipts() == null || request.getReceipts().isEmpty()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.receipt.upload.invalid");
    }
    String operatorName = resolveOperatorName(authentication);
    List<ProcurementReceiptResponse> created = new ArrayList<>();
    for (ProcurementReceiptUploadRequest.ReceiptImagePayload payload : request.getReceipts()) {
      if (payload == null || payload.getImageBase64() == null || payload.getImageBase64().isBlank()) {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.receipt.upload.invalid");
      }
      ImageStorageService.UploadResult upload = imageStorageService.saveBase64ToObject(payload.getImageBase64());
      LocalDateTime now = LocalDateTime.now();
      ProcurementReceipt receipt = new ProcurementReceipt(
          tripId,
          payload.getFileName(),
          upload == null ? "INLINE-" + now.toString() : upload.objectKey(),
          upload == null ? payload.getImageBase64() : upload.publicUrl(),
          upload == null ? "INLINE-THUMB-" + now.toString() : upload.objectKey(),
          upload == null ? payload.getImageBase64() : upload.publicUrl(),
          RECEIPT_STATUS_READY,
          now,
          now,
          buildMockReceiptReconciliation(tripId));
      ProcurementReceipt saved = procurementReceiptRepository.save(receipt);
      financialAuditTrailService.logProcurementReceiptUpload(
          tripId,
          operatorName,
          serializeAuditPayload(Map.of("receiptId", saved.getId(), "status", "NONE")),
          serializeAuditPayload(Map.of("receiptId", saved.getId(), "processingStatus", saved.getProcessingStatus())));
      created.add(toProcurementReceiptResponse(saved));
    }
    return created;
  }

  @Transactional
  public ProcurementReceiptResponse saveProcurementReceiptReconciliation(Long tripId,
                                                                         Long receiptId,
                                                                         ProcurementReceiptSaveRequest request,
                                                                         Authentication authentication) {
    ProcurementReceipt receipt = procurementReceiptRepository.findByIdAndTripId(receiptId, tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.procurement.receipt.not_found"));
    if (request == null || request.getReconciliationResult() == null) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.receipt.reconcile.invalid");
    }
    Map<String, Object> original = new LinkedHashMap<>();
    original.put("processingStatus", receipt.getProcessingStatus());
    original.put("reconciliationResult", receipt.getReconciliationResult());
    receipt.setReconciliationResult(request.getReconciliationResult());
    receipt.setProcessingStatus(request.getProcessingStatus() == null || request.getProcessingStatus().isBlank()
        ? RECEIPT_STATUS_RECONCILED
        : request.getProcessingStatus().trim());
    receipt.setUpdatedAt(LocalDateTime.now());
    ProcurementReceipt saved = procurementReceiptRepository.save(receipt);
    financialAuditTrailService.logProcurementReceiptReconciliation(
        tripId,
        resolveOperatorName(authentication),
        serializeAuditPayload(original),
        serializeAuditPayload(Map.of(
            "processingStatus", saved.getProcessingStatus(),
            "reconciliationResult", saved.getReconciliationResult(),
            "receiptId", saved.getId())));
    return toProcurementReceiptResponse(saved);
  }

  @Transactional(readOnly = true)
  public WalletSummaryResponse getWalletSummary(String partnerId) {
    var wallet = walletService.getWallet(partnerId);
    return new WalletSummaryResponse(
        wallet.getPartnerId(),
        round2(wallet.getBalance()),
        wallet.getCurrency(),
        wallet.getUpdatedAt());
  }

  @Transactional(readOnly = true)
  public List<WalletTransactionResponse> getWalletTransactions(String partnerId) {
    return walletService.getTransactions(partnerId).stream()
        .map(tx -> new WalletTransactionResponse(
            tx.getId(),
            tx.getPartnerId(),
            round2(tx.getAmount()),
            tx.getType(),
            tx.getTripId(),
            tx.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderHeader getTripOrderByBusinessId(Long tripId, String businessId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId).stream()
        .filter(order -> Objects.equals(order.getBusinessId(), businessId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
  }

  private CustomerBalanceLedgerResponse buildLedgerEntry(Trip trip, OrderHeader order) {
    List<CustomerBalanceLedgerResponse.LedgerOrderLineResponse> orderLines = safeLines(order).stream()
        .map(line -> new CustomerBalanceLedgerResponse.LedgerOrderLineResponse(
            line.getSkuId(),
            line.getItemName(),
            Math.max(line.getQuantity(), 0),
            round2(line.getUnitPrice()),
            Math.max(line.getPurchasedQuantity(), 0),
            buildDifferenceNote(line)))
        .toList();
    double totalReceivable = safeLines(order).stream()
        .mapToDouble(line -> {
          int settledQty = Math.max(line.getPurchasedQuantity(), 0) > 0 ? line.getPurchasedQuantity() : line.getQuantity();
          return settledQty * line.getUnitPrice();
        })
        .sum();
    double paidDeposit = order.getPaymentStatus() == PaymentStatus.PAID ? totalReceivable : 0D;
    return new CustomerBalanceLedgerResponse(
        trip.getId(),
        order.getBusinessId(),
        order.getCustomerId(),
        round2(totalReceivable),
        round2(paidDeposit),
        round2(Math.max(totalReceivable - paidDeposit, 0D)),
        resolveSettlementStatus(order),
        trip.isSettlementFrozen(),
        trip.getSettlementFreezeStage(),
        trip.getSettlementFreezeReason(),
        order.getReceiptConfirmedAt(),
        order.getReceiptConfirmedBy(),
        order.getBillingConfirmedAt(),
        order.getBillingConfirmedBy(),
        orderLines);
  }

  private String resolveSettlementStatus(OrderHeader order) {
    if (order.getBillingConfirmedAt() != null) {
      return "BILLING_CONFIRMED";
    }
    if (order.getReceiptConfirmedAt() != null) {
      return "RECEIPT_CONFIRMED";
    }
    if (order.getPaymentStatus() == PaymentStatus.PAID) {
      return "DEPOSIT_PAID";
    }
    return "PENDING_CONFIRMATION";
  }

  private String buildDifferenceNote(OrderLine line) {
    int orderedQuantity = Math.max(line.getQuantity(), 0);
    int purchasedQuantity = Math.max(line.getPurchasedQuantity(), 0);
    if (purchasedQuantity == 0) {
      return "Pending procurement";
    }
    if (purchasedQuantity < orderedQuantity) {
      return "Short shipped " + (orderedQuantity - purchasedQuantity);
    }
    return "Matched";
  }

  private void ensureTripMutable(Long tripId) {
    if (tripId == null) {
      return;
    }
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    if (trip.isSettlementFrozen()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.trip.settlement_frozen");
    }
  }

  private String resolveOperatorName(Authentication authentication) {
    if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
      return "SYSTEM";
    }
    return authentication.getName();
  }

  private ProcurementReceiptResponse toProcurementReceiptResponse(ProcurementReceipt item) {
    return new ProcurementReceiptResponse(
        item.getId(),
        item.getTripId(),
        item.getFileName(),
        item.getOriginalImageUrl(),
        item.getThumbnailUrl(),
        item.getProcessingStatus(),
        item.getUploadedAt(),
        item.getUpdatedAt(),
        item.getReconciliationResult());
  }

  private Map<String, Object> buildMockReceiptReconciliation(Long tripId) {
    List<OrderHeader> orders = orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId);
    List<Map<String, Object>> receiptItems = new ArrayList<>();
    List<Map<String, Object>> matchedOrderLines = new ArrayList<>();
    List<Map<String, Object>> missingOrderedItems = new ArrayList<>();
    for (OrderHeader order : orders) {
      for (OrderLine line : safeLines(order)) {
        int orderedQuantity = Math.max(line.getQuantity(), 0);
        int purchasedQuantity = Math.max(line.getPurchasedQuantity(), 0);
        receiptItems.add(Map.of(
            "name", line.getItemName(),
            "quantity", Math.max(purchasedQuantity, 1),
            "unitPrice", round2(line.getUnitPrice())));
        matchedOrderLines.add(Map.of(
            "businessId", order.getBusinessId(),
            "skuId", line.getSkuId(),
            "itemName", line.getItemName(),
            "orderedQuantity", orderedQuantity,
            "purchasedQuantity", purchasedQuantity,
            "matchedQuantity", Math.max(purchasedQuantity, Math.min(orderedQuantity, 1))));
        if (purchasedQuantity < orderedQuantity) {
          missingOrderedItems.add(Map.of(
              "businessId", order.getBusinessId(),
              "skuId", line.getSkuId(),
              "itemName", line.getItemName(),
              "missingQuantity", orderedQuantity - purchasedQuantity,
              "disposition", "OUT_OF_STOCK"));
        }
      }
    }
    List<Map<String, Object>> unmatchedReceiptItems = new ArrayList<>();
    unmatchedReceiptItems.add(new LinkedHashMap<>(Map.of(
        "name", "Store sample item",
        "quantity", 1,
        "unitPrice", 0D,
        "disposition", "UNREVIEWED")));
    return new LinkedHashMap<>(Map.of(
        "receiptItems", receiptItems,
        "matchedOrderLines", matchedOrderLines,
        "unmatchedReceiptItems", unmatchedReceiptItems,
        "missingOrderedItems", missingOrderedItems,
        "selfUseItems", new ArrayList<>()));
  }

  private TripProfitShareConfig getOrCreateProfitShareConfig(Long tripId) {
    return tripProfitShareConfigRepository.findById(tripId)
        .orElseGet(() -> new TripProfitShareConfig(
            tripId,
            DEFAULT_PURCHASER_RATIO,
            DEFAULT_PROMOTER_RATIO,
            LocalDateTime.now()));
  }

  private List<PartnerProfitShareResponse> calculatePartnerShares(double netProfit, TripProfitShareConfig config) {
    double purchaserShare = round2(netProfit * config.getPurchaserRatioPercent() / 100D);
    double promoterShare = round2(netProfit * config.getPromoterRatioPercent() / 100D);
    return List.of(
        new PartnerProfitShareResponse("PURCHASER", round2(config.getPurchaserRatioPercent()), purchaserShare),
        new PartnerProfitShareResponse("PROMOTER", round2(config.getPromoterRatioPercent()), promoterShare));
  }

  private LogisticsTrackingResponse toLogisticsResponse(TripLogisticsTracking tracking) {
    return new LogisticsTrackingResponse(
        tracking.getId(),
        tracking.getTripId(),
        tracking.getTrackingNumber(),
        tracking.getChannel().name(),
        tracking.getProvider().name(),
        tracking.getStatus().name(),
        tracking.getLastMessage(),
        tracking.isSettlementReminderTriggered(),
        tracking.getLastCheckedAt());
  }

  private LogisticsChannel parseChannel(String rawChannel) {
    if (rawChannel == null || rawChannel.isBlank()) {
      return LogisticsChannel.DOMESTIC;
    }
    try {
      return LogisticsChannel.valueOf(rawChannel.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.logistics.invalid_channel");
    }
  }

  private LogisticsProvider parseProvider(String rawProvider) {
    if (rawProvider == null || rawProvider.isBlank()) {
      return LogisticsProvider.MOCK;
    }
    try {
      return LogisticsProvider.valueOf(rawProvider.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.procurement.logistics.invalid_provider");
    }
  }

  private LogisticsStatus resolveLogisticsStatus(TripLogisticsTracking tracking) {
    String normalized = tracking.getTrackingNumber() == null ? "" : tracking.getTrackingNumber().trim().toUpperCase();
    if (tracking.getProvider() == LogisticsProvider.TRACK17) {
      return LogisticsStatus.PENDING;
    }
    if (normalized.contains("DELIVERED") || normalized.endsWith("DLV")) {
      return LogisticsStatus.DELIVERED;
    }
    if (normalized.contains("EXCEPTION") || normalized.contains("FAILED")) {
      return LogisticsStatus.EXCEPTION;
    }
    return LogisticsStatus.IN_TRANSIT;
  }

  private String resolveLogisticsMessage(TripLogisticsTracking tracking, LogisticsStatus status) {
    if (tracking.getProvider() == LogisticsProvider.TRACK17) {
      return "17track gateway reserved, using mock fallback";
    }
    return switch (status) {
      case DELIVERED -> "Package delivered, settlement reminder triggered";
      case EXCEPTION -> "Package exception detected";
      case IN_TRANSIT -> "Package in transit";
      case PENDING -> "Pending";
    };
  }

  private List<OrderLine> safeLines(OrderHeader order) {
    return order.getLines() == null ? List.of() : order.getLines();
  }

  private Optional<Product> resolveProduct(String skuId) {
    if (skuId == null || skuId.isBlank()) {
      return Optional.empty();
    }
    return productRepository.findById(skuId);
  }

  private Map<String, Double> calculateCategoryCompletion(Map<String, Integer> categoryExpected,
                                                          Map<String, Integer> categoryPurchased) {
    Map<String, Double> completion = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : categoryExpected.entrySet()) {
      int expected = Math.max(entry.getValue(), 0);
      int purchased = Math.max(categoryPurchased.getOrDefault(entry.getKey(), 0), 0);
      double percent = expected == 0 ? 0D : (purchased * 100D / expected);
      completion.put(entry.getKey(), round2(Math.min(percent, 100D)));
    }
    return completion;
  }

  private double safeRate(double rate, String rateName) {
    if (rate > 0) {
      return rate;
    }
    log.warn("Invalid FX rate '{}' configured as {}, fallback to 1.0", rateName, rate);
    return DEFAULT_FX_RATE;
  }

  private double sanitizePhysicalMetric(Double metric) {
    if (metric == null || metric < 0D) {
      return 0D;
    }
    return metric;
  }

  private double round2(double value) {
    return Math.round(value * 100D) / 100D;
  }

  private String serializeAuditPayload(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize audit payload", ex);
    }
  }

  private static final class DeficitAggregate {
    private final String skuId;
    private final String itemName;
    private int required;
    private int purchased;
    private int deficit;
    private LocalDateTime firstSeenAt;

    private DeficitAggregate(String skuId, String itemName, LocalDateTime firstSeenAt) {
      this.skuId = skuId;
      this.itemName = itemName;
      this.firstSeenAt = firstSeenAt;
    }

    private double completionPercent() {
      if (required <= 0) {
        return 0D;
      }
      return Math.min((purchased * 100D) / required, 100D);
    }

    private String priority() {
      double completion = completionPercent();
      if (deficit >= 5 || completion < 35D) {
        return "HIGH";
      }
      if (deficit >= 2 || completion < 75D) {
        return "MEDIUM";
      }
      return "LOW";
    }

    private int priorityRank() {
      return switch (priority()) {
        case "HIGH" -> 0;
        case "MEDIUM" -> 1;
        default -> 2;
      };
    }
  }

  public record ReconcileInventoryResult(int reconciledQuantity, Long tripId, List<String> allocatedBusinessIds) {
    public static ReconcileInventoryResult empty() {
      return new ReconcileInventoryResult(0, null, List.of());
    }
  }
}
