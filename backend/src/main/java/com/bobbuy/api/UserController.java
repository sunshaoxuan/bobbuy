package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
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
  public ApiResponse<List<User>> list() {
    List<User> users = store.listUsers();
    return ApiResponse.success(users, new ApiMeta(users.size()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<User>> get(@PathVariable Long id) {
    User user = store.getUser(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    return ResponseEntity.ok(ApiResponse.success(user));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<User>> create(@Valid @RequestBody User user) {
    return ResponseEntity.ok(ApiResponse.success(store.createUser(user)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<User>> update(@PathVariable Long id, @Valid @RequestBody User user) {
    return store.updateUser(id, user)
        .map(updated -> ResponseEntity.ok(ApiResponse.success(updated)))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
    if (store.deleteUser(id)) {
      return ResponseEntity.ok(ApiResponse.success(null));
    }
    throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
  }
}
