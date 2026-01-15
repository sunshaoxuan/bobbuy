package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Trip;
import com.bobbuy.service.BobbuyStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {
  private final BobbuyStore store;

  public TripController(BobbuyStore store) {
    this.store = store;
  }

  @GetMapping
  public ApiResponse<List<Trip>> list() {
    List<Trip> trips = store.listTrips();
    return ApiResponse.success(trips, new ApiMeta(trips.size()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<Trip>> get(@PathVariable Long id) {
    Trip trip = store.getTrip(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    return ResponseEntity.ok(ApiResponse.success(trip));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Trip>> create(@Valid @RequestBody Trip trip) {
    return ResponseEntity.ok(ApiResponse.success(store.createTrip(trip)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Trip>> update(@PathVariable Long id, @Valid @RequestBody Trip trip) {
    return store.updateTrip(id, trip)
        .map(updated -> ResponseEntity.ok(ApiResponse.success(updated)))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<Trip>> updateStatus(@PathVariable Long id, @Valid @RequestBody TripStatusRequest request) {
    return ResponseEntity.ok(ApiResponse.success(store.updateTripStatus(id, request.getStatus())));
  }

  @PostMapping("/{id}/reserve")
  public ResponseEntity<ApiResponse<Trip>> reserve(@PathVariable Long id, @Valid @RequestBody TripReserveRequest request) {
    return ResponseEntity.ok(ApiResponse.success(store.reserveTripCapacity(id, request.getQuantity())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    if (store.deleteTrip(id)) {
      return ResponseEntity.ok(ApiResponse.success(null));
    }
    throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found");
  }
}
