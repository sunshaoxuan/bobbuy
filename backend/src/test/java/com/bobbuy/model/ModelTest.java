package com.bobbuy.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {
    @Test
    void userTest() {
        User user = new User(1L, "Name", Role.CUSTOMER, 4.5);
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("Name");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getRating()).isEqualTo(4.5);

        user.setName("New");
        user.setRole(Role.AGENT);
        user.setRating(3.0);
        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getRole()).isEqualTo(Role.AGENT);
        assertThat(user.getRating()).isEqualTo(3.0);
    }

    @Test
    void tripTest() {
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip(1L, 100L, "O", "D", LocalDate.MAX, 10, 2, TripStatus.PUBLISHED, now);
        assertThat(trip.getOrigin()).isEqualTo("O");
        assertThat(trip.getCapacity()).isEqualTo(10);
        assertThat(trip.getReservedCapacity()).isEqualTo(2);
        assertThat(trip.getRemainingCapacity()).isEqualTo(8);
        assertThat(trip.getAgentId()).isEqualTo(100L);
        assertThat(trip.getDepartDate()).isEqualTo(LocalDate.MAX);

        trip.setDestination("D2");
        trip.setStatus(TripStatus.COMPLETED);
        trip.setOrigin("O2");
        trip.setCapacity(12);
        trip.setReservedCapacity(3);
        assertThat(trip.getDestination()).isEqualTo("D2");
        assertThat(trip.getOrigin()).isEqualTo("O2");
        assertThat(trip.getCapacity()).isEqualTo(12);
        assertThat(trip.getReservedCapacity()).isEqualTo(3);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getStatusUpdatedAt()).isEqualTo(now);
    }

    @Test
    void orderHeaderAndLineTest() {
        OrderHeader header = new OrderHeader("BUS-001", 100L, 200L);
        header.setId(10L);
        header.setStatus(OrderStatus.CONFIRMED);
        header.setTotalAmount(99.5);
        header.setStatusUpdatedAt(LocalDateTime.now());
        header.setPaymentMethod(PaymentMethod.ALIPAY);
        header.setPaymentStatus(PaymentStatus.PAID);

        OrderLine line1 = new OrderLine("SKU-1", "A", "Red", 1, 10.0);
        OrderLine line2 = new OrderLine("SKU-1", "A", "Red", 2, 10.0);
        OrderLine line3 = new OrderLine("SKU-1", "A", "Blue", 1, 10.0);

        header.addLine(line1);
        assertThat(header.getLines()).hasSize(1);
        assertThat(line1.getHeaderId()).isEqualTo(10L);
        assertThat(header.getBusinessId()).isEqualTo("BUS-001");
        assertThat(header.getCustomerId()).isEqualTo(100L);
        assertThat(header.getTripId()).isEqualTo(200L);
        assertThat(header.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(header.getTotalAmount()).isEqualTo(99.5);
        assertThat(header.getPaymentMethod()).isEqualTo(PaymentMethod.ALIPAY);
        assertThat(header.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        // Test merge logic
        assertThat(line1.canMergeWith(line2)).isTrue();
        assertThat(line1.canMergeWith(line3)).isFalse(); // Different spec
        assertThat(line1.canMergeWith(null)).isFalse();
        assertThat(line1.getItemName()).isEqualTo("A");
        assertThat(line1.getUnitPrice()).isEqualTo(10.0);
        assertThat(line1.getQuantity()).isEqualTo(1);
    }

    @Test
    void auditLogTest() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = new AuditLog(1L, "T", 100L, "A", "B", "A", 0L, now);
        assertThat(log.getEntityType()).isEqualTo("T");
        assertThat(log.getAfterValue()).isEqualTo("A");
        assertThat(log.getId()).isEqualTo(1L);
        assertThat(log.getEntityId()).isEqualTo(100L);
        assertThat(log.getBeforeValue()).isEqualTo("B");
        assertThat(log.getUserId()).isEqualTo(0L);
        assertThat(log.getCreatedAt()).isEqualTo(now);

        log.setAction("UPDATED");
        assertThat(log.getAction()).isEqualTo("UPDATED");
    }

    @Test
    void metricsTest() {
        Metrics metrics = new Metrics(1, 2, 3, 4.5,
                java.util.Map.of("NEW", 1),
                java.util.Map.of("GET /api/orders", 120L),
                java.util.Map.of("GET /api/orders", 180L),
                java.util.List.of("GET /api/orders"));
        assertThat(metrics.getUsers()).isEqualTo(1);
        assertThat(metrics.getTrips()).isEqualTo(2);
        assertThat(metrics.getOrders()).isEqualTo(3);
        assertThat(metrics.getGmV()).isEqualTo(4.5);
        assertThat(metrics.getOrderStatusCounts()).containsEntry("NEW", 1);
        assertThat(metrics.getLatencyP95Ms()).containsEntry("GET /api/orders", 120L);
        assertThat(metrics.getLatencyP99Ms()).containsEntry("GET /api/orders", 180L);
        assertThat(metrics.getSlowEndpoints()).contains("GET /api/orders");
    }

    @Test
    void productAndRelatedMasterDataModelsTest() {
        Category category = new Category(
                "cat-1",
                Map.of("zh-CN", "茶饮"),
                Map.of("zh-CN", "茶类"),
                List.of(Map.of("name", "origin", "type", "text")));
        assertThat(category.getId()).isEqualTo("cat-1");
        assertThat(category.getName()).containsEntry("zh-CN", "茶饮");

        Supplier supplier = new Supplier(
                "sup-1",
                Map.of("zh-CN", "供应商"),
                Map.of("zh-CN", "描述"),
                "supplier@bobbuy.com");
        assertThat(supplier.getContactInfo()).contains("@");

        MediaGalleryItem media = new MediaGalleryItem(
                "https://cdn.example/image.png",
                MediaType.IMAGE,
                Map.of("zh-CN", "主图"));
        Product product = new Product(
                "prd-1",
                Map.of("zh-CN", "抹茶套装"),
                Map.of("zh-CN", "描述"),
                "BrandX",
                19.9,
                List.of(media),
                StorageCondition.AMBIENT,
                OrderMethod.DIRECT_BUY,
                "cat-1",
                Map.of("sup-1", "SKU-001"));
        assertThat(product.getName()).containsEntry("zh-CN", "抹茶套装");
        assertThat(product.getMediaGallery()).hasSize(1);
        assertThat(product.getStorageCondition()).isEqualTo(StorageCondition.AMBIENT);
        assertThat(product.getMerchantSkus()).containsEntry("sup-1", "SKU-001");

        MerchantSku sku = new MerchantSku("msku-1", "prd-1", "sup-1", "SKU-001", 18.8, StockStatus.IN_STOCK);
        assertThat(sku.getSkuCode()).isEqualTo("SKU-001");
        assertThat(sku.getStockStatus()).isEqualTo(StockStatus.IN_STOCK);
    }
}
