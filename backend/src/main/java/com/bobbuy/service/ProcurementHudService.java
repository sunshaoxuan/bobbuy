package com.bobbuy.service;

import com.bobbuy.api.ProcurementDeficitItemResponse;
import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.api.CustomerBalanceLedgerResponse;
import com.bobbuy.api.FinancialAuditLogResponse;
import com.bobbuy.api.TripExpenseRequest;
import com.bobbuy.api.TripExpenseResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.PaymentStatus;
import com.bobbuy.model.Product;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripExpense;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.TripRepository;
import com.bobbuy.repository.TripExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Service
public class ProcurementHudService {
  private static final String UNCATEGORIZED = "UNCATEGORIZED";
  private static final double DEFAULT_FX_RATE = 1D;
  private static final Logger log = LoggerFactory.getLogger(ProcurementHudService.class);

  private final TripRepository tripRepository;
  private final OrderHeaderRepository orderHeaderRepository;
  private final ProductRepository productRepository;
  private final TripExpenseRepository tripExpenseRepository;
  private final FinancialAuditTrailService financialAuditTrailService;
  private final FxRateService fxRateService;
  private final double referenceFxRate;

  public ProcurementHudService(TripRepository tripRepository,
                               OrderHeaderRepository orderHeaderRepository,
                               ProductRepository productRepository,
                               TripExpenseRepository tripExpenseRepository,
                               FinancialAuditTrailService financialAuditTrailService,
                               FxRateService fxRateService,
                               @Value("${bobbuy.fx.reference-rate:1.0}") double referenceFxRate) {
    this.tripRepository = tripRepository;
    this.orderHeaderRepository = orderHeaderRepository;
    this.productRepository = productRepository;
    this.tripExpenseRepository = tripExpenseRepository;
    this.financialAuditTrailService = financialAuditTrailService;
    this.fxRateService = fxRateService;
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

    return new ProcurementHudResponse(
        tripId,
        round2(estimatedProfit),
        round2(purchasedAmount),
        round2(safeCurrentRate),
        round2(safeReferenceRate),
        round2(totalTripExpenses),
        trip.getCurrentWeight(),
        trip.getCurrentVolume(),
        calculateCategoryCompletion(categoryExpected, categoryPurchased));
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
    TripExpense expense = new TripExpense(tripId, request.getCost(), request.getCategory().trim(), LocalDateTime.now());
    TripExpense saved = tripExpenseRepository.save(expense);
    financialAuditTrailService.logExpenseCreate(
        tripId,
        "{\"cost\":0,\"category\":\"\"}",
        "{\"cost\":" + round2(saved.getCost()) + ",\"category\":\"" + saved.getCategory() + "\"}");
    return new TripExpenseResponse(saved.getId(), saved.getTripId(), round2(saved.getCost()), saved.getCategory(), saved.getCreatedAt());
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
        .map(item -> new ProcurementDeficitItemResponse(
            item.skuId,
            item.itemName,
            item.deficit,
            round2(item.completionPercent()),
            item.priority()))
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
    financialAuditTrailService.logManualReconcile(
        tripId,
        "{\"skuId\":\"" + skuId + "\",\"fromBusinessId\":\"" + fromBusinessId + "\",\"toBusinessId\":\"" + toBusinessId
            + "\",\"fromPurchased\":" + sourcePurchased + ",\"toPurchased\":" + targetPurchased + "}",
        "{\"skuId\":\"" + skuId + "\",\"fromBusinessId\":\"" + fromBusinessId + "\",\"toBusinessId\":\"" + toBusinessId
            + "\",\"fromPurchased\":" + sourceLine.getPurchasedQuantity() + ",\"toPurchased\":" + targetLine.getPurchasedQuantity()
            + ",\"transferred\":" + transferred + "}");
    return transferred;
  }

  @Transactional(readOnly = true)
  public List<FinancialAuditLogResponse> getFinancialAuditLogs(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return financialAuditTrailService.listByTripId(tripId);
  }

  @Transactional(readOnly = true)
  public List<CustomerBalanceLedgerResponse> getCustomerBalanceLedger(Long tripId) {
    tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId).stream()
        .map(order -> {
          double totalReceivable = safeLines(order).stream()
              .mapToDouble(line -> {
                int settledQty = Math.max(line.getPurchasedQuantity(), 0) > 0 ? line.getPurchasedQuantity() : line.getQuantity();
                return settledQty * line.getUnitPrice();
              })
              .sum();
          double paidDeposit = order.getPaymentStatus() == PaymentStatus.PAID ? totalReceivable : 0D;
          return new CustomerBalanceLedgerResponse(
              order.getBusinessId(),
              order.getCustomerId(),
              round2(totalReceivable),
              round2(paidDeposit),
              round2(Math.max(totalReceivable - paidDeposit, 0D)));
        })
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
