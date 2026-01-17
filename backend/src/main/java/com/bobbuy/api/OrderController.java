package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.service.BobbuyStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final BobbuyStore store;

  public OrderController(BobbuyStore store) {
    this.store = store;
  }

  @GetMapping
  public ApiResponse<List<OrderHeader>> list() {
    List<OrderHeader> orders = store.listOrders();
    return ApiResponse.success(orders, new ApiMeta(orders.size()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<OrderHeader>> get(@PathVariable Long id) {
    OrderHeader order = store.getOrder(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
    return ResponseEntity.ok(ApiResponse.success(order));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<OrderHeader>> create(@Valid @RequestBody OrderHeader order) {
    return ResponseEntity.ok(ApiResponse.success(store.upsertOrder(order)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<OrderHeader>> update(@PathVariable Long id, @Valid @RequestBody OrderHeader order) {
    return store.updateOrder(id, order)
        .map(updated -> ResponseEntity.ok(ApiResponse.success(updated)))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<OrderHeader>> updateStatus(@PathVariable Long id,
      @Valid @RequestBody OrderStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(store.updateOrderStatus(id, request.getStatus())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    if (store.deleteOrder(id)) {
      return ResponseEntity.ok(ApiResponse.success(null));
    }
    throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found");
  }
}
