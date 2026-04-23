package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Role;
import com.bobbuy.model.User;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserControllerTest {
  @Autowired
  private BobbuyStore store;
  @Autowired
  private UserController controller;

  @BeforeEach
  void setUp() {
    store.seed();
  }

  @Test
  void userCrudFlow() {
    ApiResponse<java.util.List<User>> list = controller.list();
    assertThat(list.getData()).hasSize(2);
    assertThat(list.getData().get(0).getDefaultAddress()).isNotNull();
    assertThat(list.getData().get(0).getSocialAccounts()).isNotEmpty();

    ResponseEntity<ApiResponse<User>> fetched = controller.get(1000L);
    assertThat(fetched.getBody()).isNotNull();
    assertThat(fetched.getBody().getData().getId()).isEqualTo(1000L);
    assertThat(fetched.getBody().getData().getPhone()).isNotBlank();

    User payload = new User(null, "New User", Role.CUSTOMER, 5.0);
    payload.setPhone("+86-13900000000");
    payload.setEmail("new@bobbuy.test");
    payload.setNote("VIP");
    User.UserAddress address = new User.UserAddress();
    address.setContactName("New User");
    address.setPhone("+86-13900000000");
    address.setCountryRegion("China");
    address.setCity("Shanghai");
    address.setAddressLine("Century Ave 1");
    address.setPostalCode("200000");
    payload.setDefaultAddress(address);
    User.UserSocialAccount social = new User.UserSocialAccount();
    social.setPlatform("WeChat");
    social.setHandle("new-user");
    social.setDisplayName("New User");
    social.setVerified(false);
    social.setNote("Registration only");
    payload.setSocialAccounts(java.util.List.of(social));
    User created = controller.create(payload).getBody().getData();
    assertThat(created.getId()).isNotNull();
    assertThat(created.getDefaultAddress().getCity()).isEqualTo("Shanghai");
    assertThat(created.getSocialAccounts()).hasSize(1);

    User updatePayload = new User(null, "Updated", Role.AGENT, 4.2);
    updatePayload.setPhone("+81-9011111111");
    updatePayload.setEmail("updated@bobbuy.test");
    updatePayload.setNote("Escalation owner");
    updatePayload.setDefaultAddress(address);
    updatePayload.setSocialAccounts(java.util.List.of(social));
    ResponseEntity<ApiResponse<User>> updated = controller.update(created.getId(), updatePayload);
    assertThat(updated.getBody().getData().getName()).isEqualTo("Updated");
    assertThat(updated.getBody().getData().getEmail()).isEqualTo("updated@bobbuy.test");

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
