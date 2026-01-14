package com.bobbuy.api;

import com.bobbuy.model.Trip;
import com.bobbuy.service.BobbuyStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
  public List<Trip> list() {
    return store.listTrips();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Trip> get(@PathVariable Long id) {
    return store.getTrip(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Trip> create(@Valid @RequestBody Trip trip) {
    return ResponseEntity.ok(store.createTrip(trip));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Trip> update(@PathVariable Long id, @Valid @RequestBody Trip trip) {
    return store.updateTrip(id, trip)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (store.deleteTrip(id)) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }
}
