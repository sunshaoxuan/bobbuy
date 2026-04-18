package com.bobbuy.service;

import com.bobbuy.api.ProcurementDeficitItemResponse;
import com.bobbuy.api.ProcurementHudResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Product;
import com.bobbuy.model.Trip;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProcurementHudService {
  private static final String UNCATEGORIZED = "UNCATEGORIZED";

  private final TripRepository tripRepository;
  private final OrderHeaderRepository orderHeaderRepository;
  private final ProductRepository productRepository;
  private final double referenceFxRate;
  private final double currentFxRate;

  public ProcurementHudService(TripRepository tripRepository,
                               OrderHeaderRepository orderHeaderRepository,
                               ProductRepository productRepository,
                               @Value("${bobbuy.fx.reference-rate:1.0}") double referenceFxRate,
                               @Value("${bobbuy.fx.current-rate:${bobbuy.fx.reference-rate:1.0}}") double currentFxRate) {
    this.tripRepository = tripRepository;
    this.orderHeaderRepository = orderHeaderRepository;
    this.productRepository = productRepository;
    this.referenceFxRate = referenceFxRate;
    this.currentFxRate = currentFxRate;
  }

  @Transactional(readOnly = true)
  public ProcurementHudResponse getHudStats(Long tripId) {
    Trip trip = tripRepository.findById(tripId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    List<OrderHeader> orders = orderHeaderRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId);

    double purchasedAmount = 0D;
    double expectedRevenue = 0D;
    int totalPurchasedUnits = 0;

    Map<String, Integer> categoryExpected = new LinkedHashMap<>();
    Map<String, Integer> categoryPurchased = new LinkedHashMap<>();

    for (OrderHeader order : orders) {
      for (OrderLine line : safeLines(order)) {
        int required = Math.max(line.getQuantity(), 0);
        int purchased = Math.min(Math.max(line.getPurchasedQuantity(), 0), required);

        expectedRevenue += required * line.getUnitPrice() * safeRate(referenceFxRate);
        purchasedAmount += purchased * line.getUnitPrice() * safeRate(currentFxRate);
        totalPurchasedUnits += purchased;

        String categoryId = resolveCategory(line.getSkuId());
        categoryExpected.merge(categoryId, required, Integer::sum);
        categoryPurchased.merge(categoryId, purchased, Integer::sum);
      }
    }

    trip.recalculateCurrentLoad(totalPurchasedUnits, 1D, 1D);
    double estimatedProfit = expectedRevenue - purchasedAmount;

    return new ProcurementHudResponse(
        tripId,
        round2(estimatedProfit),
        round2(purchasedAmount),
        trip.getCurrentWeight(),
        trip.getCurrentVolume(),
        calculateCategoryCompletion(categoryExpected, categoryPurchased));
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
    if (productId == null || productId.isBlank() || quantity <= 0) {
      return 0;
    }

    List<OrderHeader> draftOrders = orderHeaderRepository.findByStatusForUpdate(OrderStatus.NEW);
    List<OrderHeader> candidateOrders = draftOrders.stream()
        .filter(order -> safeLines(order).stream().anyMatch(line -> Objects.equals(line.getSkuId(), productId)))
        .sorted(Comparator
            .comparing(OrderHeader::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OrderHeader::getId))
        .toList();

    if (candidateOrders.isEmpty()) {
      return 0;
    }

    Long targetTripId = candidateOrders.get(0).getTripId();
    int remaining = quantity;
    int reconciled = 0;
    List<OrderHeader> changedOrders = new ArrayList<>();

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
    return reconciled;
  }

  private List<OrderLine> safeLines(OrderHeader order) {
    return order.getLines() == null ? List.of() : order.getLines();
  }

  private String resolveCategory(String skuId) {
    return productRepository.findById(skuId)
        .map(Product::getCategoryId)
        .filter(value -> value != null && !value.isBlank())
        .orElse(UNCATEGORIZED);
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

  private double safeRate(double rate) {
    return rate > 0 ? rate : 1D;
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
}
