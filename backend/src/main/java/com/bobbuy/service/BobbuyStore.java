package com.bobbuy.service;

import com.bobbuy.api.OrderPlacementRequest;
import com.bobbuy.api.ProcurementItemResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Category;
import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.MediaType;
import com.bobbuy.model.MerchantSku;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.PaymentStatus;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductPatch;
import com.bobbuy.model.Role;
import com.bobbuy.model.StockStatus;
import com.bobbuy.model.StorageCondition;
import com.bobbuy.model.Supplier;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.model.User;
import com.bobbuy.repository.CategoryRepository;
import com.bobbuy.repository.FinancialAuditLogRepository;
import com.bobbuy.repository.MerchantSkuRepository;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.SupplierRepository;
import com.bobbuy.repository.TripRepository;
import com.bobbuy.repository.TripExpenseRepository;
import com.bobbuy.repository.TripLogisticsTrackingRepository;
import com.bobbuy.repository.TripProfitShareConfigRepository;
import com.bobbuy.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class BobbuyStore {
    private static final Long SYSTEM_USER_ID = 0L;

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final MerchantSkuRepository merchantSkuRepository;
    private final TripExpenseRepository tripExpenseRepository;
    private final TripLogisticsTrackingRepository tripLogisticsTrackingRepository;
    private final TripProfitShareConfigRepository tripProfitShareConfigRepository;
    private final FinancialAuditLogRepository financialAuditLogRepository;
    private final AuditLogService auditLogService;
    private final double unitWeight;
    private final double unitVolume;
    private final AtomicLong orderIdentity = new AtomicLong(3000L);

    public BobbuyStore(
            UserRepository userRepository,
            TripRepository tripRepository,
            OrderHeaderRepository orderHeaderRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            MerchantSkuRepository merchantSkuRepository,
            TripExpenseRepository tripExpenseRepository,
            TripLogisticsTrackingRepository tripLogisticsTrackingRepository,
            TripProfitShareConfigRepository tripProfitShareConfigRepository,
            FinancialAuditLogRepository financialAuditLogRepository,
            AuditLogService auditLogService,
            @Value("${bobbuy.trip.unit-weight:1.0}") double unitWeight,
            @Value("${bobbuy.trip.unit-volume:1.0}") double unitVolume) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.orderHeaderRepository = orderHeaderRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.merchantSkuRepository = merchantSkuRepository;
        this.tripExpenseRepository = tripExpenseRepository;
        this.tripLogisticsTrackingRepository = tripLogisticsTrackingRepository;
        this.tripProfitShareConfigRepository = tripProfitShareConfigRepository;
        this.financialAuditLogRepository = financialAuditLogRepository;
        this.auditLogService = auditLogService;
        this.unitWeight = unitWeight;
        this.unitVolume = unitVolume;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        merchantSkuRepository.deleteAll();
        productRepository.deleteAll();
        supplierRepository.deleteAll();
        categoryRepository.deleteAll();
        financialAuditLogRepository.deleteAll();
        tripLogisticsTrackingRepository.deleteAll();
        tripProfitShareConfigRepository.deleteAll();
        tripExpenseRepository.deleteAll();
        orderHeaderRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();

        User agent = new User(1000L, "Aiko Tan", Role.AGENT, 4.8);
        User customer = new User(1001L, "Chen Li", Role.CUSTOMER, 4.6);
        userRepository.save(agent);
        userRepository.save(customer);

        Trip trip = new Trip(2000L, agent.getId(), "Tokyo", "Shanghai", LocalDate.now().plusDays(5),
                6, 1,
                TripStatus.PUBLISHED, LocalDateTime.now());
        tripRepository.save(trip);

        OrderHeader header = new OrderHeader("20260117001", customer.getId(), trip.getId());
        header.setId(3000L);
        OrderLine line = new OrderLine("SKU001", "Matcha Kit", null, 2, 32.5);
        header.addLine(line);
        header.setTotalAmount(65.0);
        orderHeaderRepository.save(header);

        Category foodCategory = new Category(
                "cat-1000",
                new LinkedHashMap<>(Map.of("zh-CN", "食品", "ja-JP", "食品", "en-US", "Food")),
                new LinkedHashMap<>(Map.of("zh-CN", "食品类商品", "ja-JP", "食品カテゴリ", "en-US", "Food products")),
                List.of(
                        categoryTemplateField("shelfLifeDays", "stock.dynamic.shelf_life_days", "number"),
                        categoryTemplateField("storageTemp", "stock.dynamic.storage_temp", "text"),
                        categoryTemplateField("flavor", "stock.dynamic.flavor", "text")));
        categoryRepository.save(foodCategory);

        Category clothingCategory = new Category(
                "cat-1001",
                new LinkedHashMap<>(Map.of("zh-CN", "服装", "ja-JP", "衣料品", "en-US", "Clothing")),
                new LinkedHashMap<>(Map.of("zh-CN", "服装类商品", "ja-JP", "衣料品カテゴリ", "en-US", "Clothing products")),
                List.of(
                        categoryTemplateFieldWithOptions("size", "stock.dynamic.size", List.of("XS", "S", "M", "L", "XL")),
                        categoryTemplateField("material", "stock.dynamic.material", "text"),
                        categoryTemplateField("color", "stock.dynamic.color", "text")));
        categoryRepository.save(clothingCategory);

        Supplier supplier = new Supplier(
                "sup-1000",
                new LinkedHashMap<>(Map.of("zh-CN", "东京供货商", "ja-JP", "東京サプライヤー", "en-US", "Tokyo Supplier")),
                new LinkedHashMap<>(Map.of("zh-CN", "稳定供应抹茶", "ja-JP", "抹茶を安定供給", "en-US", "Stable matcha supplier")),
                "tokyo-supplier@bobbuy.com");
        supplierRepository.save(supplier);

        Product product = new Product(
                "prd-1000",
                new LinkedHashMap<>(Map.of("zh-CN", "抹茶套装", "ja-JP", "抹茶セット", "en-US", "Matcha Kit")),
                new LinkedHashMap<>(Map.of("zh-CN", "京都风味抹茶组合", "ja-JP", "京都風味の抹茶セット", "en-US", "Kyoto-style matcha set")),
                "BOBBuy Select",
                32.5,
                0.5,
                0.2,
                List.of(new MediaGalleryItem(
                        "https://cdn.bobbuy.example/products/matcha-kit.png",
                        MediaType.IMAGE,
                        new LinkedHashMap<>(Map.of("zh-CN", "商品主图", "ja-JP", "商品メイン画像", "en-US", "Main image")))),
                StorageCondition.AMBIENT,
                OrderMethod.DIRECT_BUY,
                foodCategory.getId(),
                new LinkedHashMap<>(Map.of(supplier.getId(), "TOKYO-MATCHA-001")));
        product.setRecommended(true);
        product.setTemporary(false);
        productRepository.save(product);

        MerchantSku merchantSku = new MerchantSku(
                "msku-1000",
                product.getId(),
                supplier.getId(),
                "TOKYO-MATCHA-001",
                30.0,
                StockStatus.IN_STOCK);
        merchantSkuRepository.save(merchantSku);
        orderIdentity.set(3000L);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        user.setId(nextUserId());
        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, User user) {
        if (!userRepository.existsById(id)) {
            return Optional.empty();
        }
        user.setId(id);
        return Optional.of(userRepository.save(user));
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    public List<Trip> listTrips() {
        return tripRepository.findAll().stream()
                .peek(this::refreshTripDynamicLoad)
                .toList();
    }

    public Optional<Trip> getTrip(Long id) {
        return tripRepository.findById(id)
                .map(trip -> {
                    refreshTripDynamicLoad(trip);
                    return trip;
                });
    }

    @Transactional
    public Trip createTrip(Trip trip) {
        trip.setId(nextTripId());
        trip.setReservedCapacity(Math.max(trip.getReservedCapacity(), 0));
        trip.setStatusUpdatedAt(LocalDateTime.now());
        return tripRepository.save(trip);
    }

    @Transactional
    public Optional<Trip> updateTrip(Long id, Trip trip) {
        if (!tripRepository.existsById(id)) {
            return Optional.empty();
        }
        trip.setId(id);
        trip.setStatusUpdatedAt(LocalDateTime.now());
        return Optional.of(tripRepository.save(trip));
    }

    @Transactional
    public Trip updateTripStatus(Long id, TripStatus nextStatus) {
        Trip trip = getTrip(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        String previousStatus = trip.getStatus().name();
        trip.setStatus(nextStatus);
        trip.setStatusUpdatedAt(LocalDateTime.now());
        Trip saved = tripRepository.save(trip);
        auditLogService.logStatusChange("TRIP", id, previousStatus, nextStatus.name(), SYSTEM_USER_ID);
        return saved;
    }

    @Transactional
    public boolean deleteTrip(Long id) {
        if (!tripRepository.existsById(id)) {
            return false;
        }
        tripRepository.deleteById(id);
        return true;
    }

    public List<Product> listProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(Product::isRecommended).reversed()
                        .thenComparing(Product::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Product::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public Optional<Product> getProduct(String id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            product.setId(UUID.randomUUID().toString());
        }
        ensureLocalizedFields(product);
        return productRepository.save(product);
    }

    @Transactional
    public Optional<Product> updateProduct(String id, Product product) {
        if (!productRepository.existsById(id)) {
            return Optional.empty();
        }
        product.setId(id);
        ensureLocalizedFields(product);
        return Optional.of(productRepository.save(product));
    }

    @Transactional
    public Optional<Product> patchProduct(String id, ProductPatch patch) {
        Optional<Product> existingOptional = productRepository.findById(id);
        if (existingOptional.isEmpty()) {
            return Optional.empty();
        }
        Product existing = existingOptional.orElseThrow();
        if (patch == null) {
            return Optional.of(existing);
        }
        if (patch.getName() != null) {
            mergeLocalizedMap(existing, true, patch.getName());
        }
        if (patch.getDescription() != null) {
            mergeLocalizedMap(existing, false, patch.getDescription());
        }
        if (patch.getBrand() != null) {
            existing.setBrand(patch.getBrand());
        }
        if (patch.getBasePrice() != null) {
            existing.setBasePrice(patch.getBasePrice());
        }
        if (patch.getWeight() != null) {
            existing.setWeight(patch.getWeight());
        }
        if (patch.getVolume() != null) {
            existing.setVolume(patch.getVolume());
        }
        if (patch.getMediaGallery() != null) {
            existing.setMediaGallery(patch.getMediaGallery());
        }
        if (patch.getStorageCondition() != null) {
            existing.setStorageCondition(patch.getStorageCondition());
        }
        if (patch.getOrderMethod() != null) {
            existing.setOrderMethod(patch.getOrderMethod());
        }
        if (patch.getCategoryId() != null) {
            existing.setCategoryId(patch.getCategoryId());
        }
        if (patch.getMerchantSkus() != null) {
            existing.setMerchantSkus(patch.getMerchantSkus());
        }
        if (patch.getPriceTiers() != null) {
            existing.setPriceTiers(patch.getPriceTiers());
        }
        if (patch.getIsRecommended() != null) {
            existing.setRecommended(patch.getIsRecommended());
        }
        if (patch.getIsTemporary() != null) {
            existing.setTemporary(patch.getIsTemporary());
        }
        return Optional.of(productRepository.save(existing));
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategory(String id) {
        return categoryRepository.findById(id);
    }

    public List<Supplier> listSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplier(String id) {
        return supplierRepository.findById(id);
    }

    public List<MerchantSku> listMerchantSkus() {
        return merchantSkuRepository.findAll();
    }

    public Optional<MerchantSku> getMerchantSku(String id) {
        return merchantSkuRepository.findById(id);
    }

    public List<OrderHeader> listOrders() {
        return listOrders(null);
    }

    public List<OrderHeader> listOrders(Long tripId) {
        if (tripId == null) {
            return orderHeaderRepository.findAll();
        }
        return orderHeaderRepository.findByTripId(tripId);
    }

    public Optional<OrderHeader> getOrder(Long id) {
        return orderHeaderRepository.findById(id);
    }

    public Optional<OrderHeader> getOrderByBusinessId(String businessId) {
        return orderHeaderRepository.findByBusinessId(businessId);
    }

    @Transactional
    public OrderHeader upsertOrder(OrderHeader headerInput) {
        if (headerInput.getPaymentStatus() == null) {
            headerInput.setPaymentStatus(PaymentStatus.UNPAID);
        }
        if (headerInput.getCreatedAt() == null) {
            headerInput.setCreatedAt(LocalDateTime.now());
        }
        if (headerInput.getLines() == null) {
            headerInput.setLines(new ArrayList<>());
        }
        normalizeLinePurchasedQuantities(headerInput.getLines());
        Optional<OrderHeader> existingOptional = orderHeaderRepository.findByBusinessIdForUpdate(headerInput.getBusinessId());

        if (existingOptional.isEmpty()) {
            ensureOrderLinesMutable(headerInput.getTripId());
            if (isAtLeastConfirmed(headerInput.getStatus()) && headerInput.getTripId() != null) {
                reserveTripCapacity(headerInput.getTripId(), calculateTotalQuantity(headerInput));
            }
            headerInput.setId(nextOrderIdentity());
            headerInput.setStatusUpdatedAt(LocalDateTime.now());
            for (OrderLine line : headerInput.getLines()) {
                line.setId(nextOrderIdentity());
                line.setHeaderId(headerInput.getId());
            }
            recalculateTotal(headerInput);
            return orderHeaderRepository.save(headerInput);
        }

        OrderHeader existing = existingOptional.orElseThrow();
        ensureOrderLinesMutable(existing.getTripId());
        if (existing.getLines() == null) {
            existing.setLines(new ArrayList<>());
        }
        int additionalQuantity = calculateTotalQuantity(headerInput);
        if (additionalQuantity > 0 && isAtLeastConfirmed(existing.getStatus()) && existing.getTripId() != null) {
            reserveTripCapacity(existing.getTripId(), additionalQuantity);
        }
        for (OrderLine newLine : headerInput.getLines()) {
            Optional<OrderLine> match = existing.getLines().stream()
                    .filter(l -> l.canMergeWith(newLine))
                    .findFirst();

            if (match.isPresent()) {
                OrderLine existingLine = match.get();
                existingLine.setQuantity(existingLine.getQuantity() + newLine.getQuantity());
                existingLine.setPurchasedQuantity(existingLine.getPurchasedQuantity() + newLine.getPurchasedQuantity());
            } else {
                newLine.setId(nextOrderIdentity());
                existing.addLine(newLine);
            }
        }
        existing.setStatusUpdatedAt(LocalDateTime.now());
        recalculateTotal(existing);
        return orderHeaderRepository.save(existing);
    }

    @Transactional
    public OrderHeader quickOrder(Long tripId, OrderPlacementRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        ensureOrderLinesMutable(trip.getId());
        Product product = productRepository.findById(request.getSkuId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.product.not_found"));

        Optional<OrderHeader> existing = orderHeaderRepository.findByBusinessIdAndTripIdForUpdate(request.getBusinessId(), tripId);

        String itemName = product.getName().getOrDefault("zh-CN", 
            product.getName().isEmpty() ? "Unknown Item" : product.getName().values().iterator().next());

        OrderLine newLine = new OrderLine(
            product.getId(),
            itemName,
            null,
            request.getQuantity(),
            product.getBasePrice()
        );

        if (existing.isPresent()) {
            OrderHeader header = existing.get();
            header.addLine(newLine); // addLine handles IDs and merging
            recalculateTotal(header);
            if (isAtLeastConfirmed(header.getStatus())) {
                reserveTripCapacity(tripId, request.getQuantity());
            }
            return orderHeaderRepository.save(header);
        } else {
            OrderHeader newHeader = new OrderHeader(request.getBusinessId(), null, tripId);
            newHeader.setTripId(tripId);
            newHeader.setStatus(OrderStatus.NEW);
            newHeader.setPaymentStatus(PaymentStatus.UNPAID);
            newHeader.addLine(newLine);
            newHeader.setId(nextOrderIdentity());
            newHeader.setCreatedAt(LocalDateTime.now());
            newHeader.setStatusUpdatedAt(LocalDateTime.now());
            recalculateTotal(newHeader);
            return orderHeaderRepository.save(newHeader);
        }
    }

    private void recalculateTotal(OrderHeader header) {
        double total = header.getLines().stream()
                .mapToDouble(l -> l.getUnitPrice() * l.getQuantity())
                .sum();
        header.setTotalAmount(total);
    }

    @Transactional
    public Optional<OrderHeader> updateOrder(Long id, OrderHeader order) {
        Optional<OrderHeader> existingOptional = orderHeaderRepository.findById(id);
        if (existingOptional.isEmpty()) {
            return Optional.empty();
        }
        OrderHeader existing = existingOptional.orElseThrow();
        ensureOrderLinesMutable(existing.getTripId());
        ensureOrderLinesMutable(order.getTripId());
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(existing.getCreatedAt() != null ? existing.getCreatedAt() : LocalDateTime.now());
        }
        if (order.getLines() == null) {
            order.setLines(new ArrayList<>());
        }
        normalizeLinePurchasedQuantities(order.getLines());
        order.setId(id);
        for (OrderLine line : order.getLines()) {
            if (line.getId() == null) {
                line.setId(nextOrderIdentity());
            }
            line.setHeaderId(id);
        }
        order.setStatusUpdatedAt(LocalDateTime.now());
        return Optional.of(orderHeaderRepository.save(order));
    }

    @Transactional
    public OrderHeader updateOrderStatus(Long id, OrderStatus nextStatus) {
        OrderHeader order = getOrder(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
        return applyOrderStatusTransition(order, nextStatus, true);
    }

    @Transactional
    public boolean deleteOrder(Long id) {
        Optional<OrderHeader> removed = orderHeaderRepository.findById(id);
        if (removed.isEmpty()) {
            return false;
        }
        ensureOrderLinesMutable(removed.orElseThrow().getTripId());
        orderHeaderRepository.deleteById(id);
        return true;
    }

    @Transactional
    public Trip reserveTripCapacity(Long id, int quantity) {
        Trip trip = tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        int remaining = trip.getRemainingCapacity();
        if (quantity <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "error.trip.invalid_quantity");
        }
        if (remaining < quantity) {
            throw new ApiException(ErrorCode.CAPACITY_NOT_ENOUGH, "error.trip.capacity_not_enough");
        }
        trip.setReservedCapacity(trip.getReservedCapacity() + quantity);
        trip.setStatusUpdatedAt(LocalDateTime.now());
        return tripRepository.save(trip);
    }

    @Transactional
    public Trip releaseTripCapacity(Long id, int quantity) {
        Trip trip = tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        if (quantity <= 0) {
            return trip;
        }
        int releaseQuantity = Math.min(quantity, trip.getReservedCapacity());
        if (releaseQuantity == 0) {
            return trip;
        }
        trip.setReservedCapacity(trip.getReservedCapacity() - releaseQuantity);
        trip.setStatusUpdatedAt(LocalDateTime.now());
        return tripRepository.save(trip);
    }

    @Transactional
    public List<OrderHeader> bulkUpdateOrderStatus(Long tripId, OrderStatus targetStatus) {
        getTrip(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        List<OrderHeader> eligible = listOrders(tripId).stream()
                .filter(order -> isValidStatusTransition(order.getStatus(), targetStatus))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) {
            return new ArrayList<>();
        }
        if (targetStatus == OrderStatus.CONFIRMED) {
            int totalQuantity = eligible.stream()
                    .mapToInt(this::calculateTotalQuantity)
                    .sum();
            if (totalQuantity > 0) {
                reserveTripCapacity(tripId, totalQuantity);
            }
        }
        List<OrderHeader> updated = new ArrayList<>();
        boolean applyCapacity = targetStatus != OrderStatus.CONFIRMED;
        for (OrderHeader order : eligible) {
            updated.add(applyOrderStatusTransition(order, targetStatus, applyCapacity));
        }
        return updated;
    }

    public List<ProcurementItemResponse> getProcurementList(Long tripId) {
        getTrip(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        Map<String, ProcurementItemResponse> aggregated = new ConcurrentHashMap<>();
        listOrders(tripId).forEach(order -> order.getLines().forEach(line -> {
            String key = line.getSkuId() + "||" + line.getItemName();
            aggregated.compute(key, (_key, existing) -> {
                if (existing == null) {
                    return new ProcurementItemResponse(
                            line.getSkuId(),
                            line.getItemName(),
                            line.getQuantity(),
                            List.of(order.getBusinessId()));
                }
                existing.addQuantity(line.getQuantity());
                existing.addBusinessId(order.getBusinessId());
                return existing;
            });
        }));
        return new ArrayList<>(aggregated.values());
    }

    public double calculateGmv() {
        return orderHeaderRepository.sumTotalAmount();
    }

    public Map<OrderStatus, Integer> orderStatusCounts() {
        Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
        orderHeaderRepository.countByStatus().forEach(item ->
                counts.put(item.getStatus(), (int) item.getTotal()));
        return counts;
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case NEW -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PURCHASED || next == OrderStatus.CANCELLED;
            case PURCHASED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.SETTLED;
            case SETTLED, CANCELLED -> false;
        };
    }

    private OrderHeader applyOrderStatusTransition(OrderHeader order, OrderStatus nextStatus, boolean applyCapacity) {
        if (!isValidStatusTransition(order.getStatus(), nextStatus)) {
            throw new ApiException(ErrorCode.INVALID_STATUS, "error.order.invalid_status");
        }
        if (applyCapacity && order.getTripId() != null && shouldReserveCapacity(order.getStatus(), nextStatus)) {
            reserveTripCapacity(order.getTripId(), calculateTotalQuantity(order));
        }
        if (applyCapacity && order.getTripId() != null && shouldReleaseCapacity(order.getStatus(), nextStatus)) {
            releaseTripCapacity(order.getTripId(), calculateTotalQuantity(order));
        }
        String previousStatus = order.getStatus().name();
        order.setStatus(nextStatus);
        order.setStatusUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orderHeaderRepository.save(order);
        auditLogService.logStatusChange("ORDER", order.getId(), previousStatus, nextStatus.name(), SYSTEM_USER_ID);
        return saved;
    }

    private boolean shouldReserveCapacity(OrderStatus current, OrderStatus nextStatus) {
        return !isAtLeastConfirmed(current) && isAtLeastConfirmed(nextStatus);
    }

    private boolean isAtLeastConfirmed(OrderStatus status) {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.PURCHASED
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.SETTLED;
    }

    private boolean shouldReleaseCapacity(OrderStatus current, OrderStatus nextStatus) {
        return nextStatus == OrderStatus.CANCELLED && isAtLeastConfirmed(current);
    }

    private void ensureOrderLinesMutable(Long tripId) {
        if (tripId == null) {
            return;
        }
        Optional<Trip> tripOptional = tripRepository.findById(tripId);
        if (tripOptional.isEmpty()) {
            return;
        }
        Trip trip = tripOptional.orElseThrow();
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.SETTLED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "error.order.locked_after_trip_completed");
        }
    }

    private int calculateTotalQuantity(OrderHeader header) {
        if (header == null || header.getLines() == null) {
            return 0;
        }
        return header.getLines().stream()
                .mapToInt(OrderLine::getQuantity)
                .sum();
    }

    private void normalizeLinePurchasedQuantities(List<OrderLine> lines) {
        if (lines == null) {
            return;
        }
        for (OrderLine line : lines) {
            int cappedPurchased = Math.min(Math.max(line.getPurchasedQuantity(), 0), Math.max(line.getQuantity(), 0));
            line.setPurchasedQuantity(cappedPurchased);
        }
    }

    private void refreshTripDynamicLoad(Trip trip) {
        if (trip == null || trip.getId() == null) {
            return;
        }
        int purchasedQuantity = orderHeaderRepository.findByTripId(trip.getId()).stream()
                .sorted(Comparator.comparing(OrderHeader::getId))
                .mapToInt(order -> order.getLines().stream()
                        .mapToInt(line -> {
                            if (line.getPurchasedQuantity() > 0) {
                                return line.getPurchasedQuantity();
                            }
                            return isAtLeastConfirmed(order.getStatus()) ? line.getQuantity() : 0;
                        })
                        .sum())
                .sum();
        trip.recalculateCurrentLoad(purchasedQuantity, unitWeight, unitVolume);
    }

    private void ensureLocalizedFields(Product product) {
        if (product.getName() == null) {
            product.setName(new HashMap<>());
        } else {
            product.setName(new HashMap<>(product.getName()));
        }
        if (product.getDescription() == null) {
            product.setDescription(new HashMap<>());
        } else {
            product.setDescription(new HashMap<>(product.getDescription()));
        }
        if (product.getMediaGallery() == null) {
            product.setMediaGallery(new ArrayList<>());
        } else {
            product.setMediaGallery(new ArrayList<>(product.getMediaGallery()));
        }
        if (product.getMerchantSkus() == null) {
            product.setMerchantSkus(new HashMap<>());
        } else {
            product.setMerchantSkus(new HashMap<>(product.getMerchantSkus()));
        }
    }

    private void mergeLocalizedMap(Product product, boolean isNameField, Map<String, String> patchValues) {
        Map<String, String> source = isNameField ? product.getName() : product.getDescription();
        Map<String, String> target = source == null ? new HashMap<>() : new HashMap<>(source);
        if (isNameField) {
            product.setName(target);
        } else {
            product.setDescription(target);
        }

        for (Map.Entry<String, String> entry : patchValues.entrySet()) {
            if (entry.getValue() == null) {
                target.remove(entry.getKey());
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<String, Object> categoryTemplateField(String key, String labelKey, String type) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", key);
        field.put("labelKey", labelKey);
        field.put("type", type);
        return field;
    }

    private Map<String, Object> categoryTemplateFieldWithOptions(String key, String labelKey, List<String> options) {
        Map<String, Object> field = categoryTemplateField(key, labelKey, "select");
        field.put("options", options);
        return field;
    }

    private Long nextUserId() {
        return userRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(1000L);
    }

    private Long nextTripId() {
        return tripRepository.findTopByOrderByIdDesc()
                .map(existing -> existing.getId() + 1)
                .orElse(2000L);
    }

    private synchronized Long nextOrderIdentity() {
        long current = orderIdentity.incrementAndGet();
        long persistedMax = orderHeaderRepository.findTopByOrderByIdDesc()
                .map(OrderHeader::getId)
                .orElse(2999L);
        if (current <= persistedMax) {
            long adjusted = persistedMax + 1;
            orderIdentity.set(adjusted);
            return adjusted;
        }
        return current;
    }
}
