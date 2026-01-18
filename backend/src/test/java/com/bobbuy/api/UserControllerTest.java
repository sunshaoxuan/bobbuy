package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Role;
import com.bobbuy.model.User;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserControllerTest {
  private BobbuyStore store;
  private UserController controller;

  @BeforeEach
  void setUp() {
    store = new BobbuyStore(new AuditLogService());
    store.seed();
    controller = new UserController(store);
  }

  @Test
  void userCrudFlow() {
    ApiResponse<java.util.List<User>> list = controller.list();
    assertThat(list.getData()).hasSize(2);

    ResponseEntity<ApiResponse<User>> fetched = controller.get(1000L);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().getData().getId()).isEqualTo(1000L);

    User created = controller.create(new User(null, "New User", Role.CUSTOMER, 5.0)).getBody().getData();
    assertThat(created.getId()).isNotNull();

    User updatePayload = new User(null, "Updated", Role.AGENT, 4.2);
    ResponseEntity<ApiResponse<User>> updated = controller.update(created.getId(), updatePayload);
    assertThat(updated.getBody().getData().getName()).isEqualTo("Updated");

    ResponseEntity<ApiResponse<Void>> deleted = controller.delete(created.getId());
    assertThat(deleted.getBody()).isNotNull();
  }

  @Test
  void userCrudRejectsMissing() {
    assertThatThrownBy(() -> controller.get(9999L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> controller.update(9999L, new User(null, "Missing", Role.CUSTOMER, 3.0)))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> controller.delete(9999L))
        .isInstanceOf(ApiException.class);
  }
}
