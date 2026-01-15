package com.bobbuy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class User {
  private Long id;

  @NotBlank(message = "{validation.user.name.required}")
  private String name;

  @NotNull(message = "{validation.user.role.required}")
  private Role role;

  private double rating;

  public User() {
  }

  public User(Long id, String name, Role role, double rating) {
    this.id = id;
    this.name = name;
    this.role = role;
    this.rating = rating;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }
}
