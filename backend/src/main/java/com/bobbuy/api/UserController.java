package com.bobbuy.api;

import com.bobbuy.model.User;
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
@RequestMapping("/api/users")
public class UserController {
  private final BobbuyStore store;

  public UserController(BobbuyStore store) {
    this.store = store;
  }

  @GetMapping
  public List<User> list() {
    return store.listUsers();
  }

  @GetMapping("/{id}")
  public ResponseEntity<User> get(@PathVariable Long id) {
    return store.getUser(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<User> create(@Valid @RequestBody User user) {
    return ResponseEntity.ok(store.createUser(user));
  }

  @PutMapping("/{id}")
  public ResponseEntity<User> update(@PathVariable Long id, @Valid @RequestBody User user) {
    return store.updateUser(id, user)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (store.deleteUser(id)) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }
}
