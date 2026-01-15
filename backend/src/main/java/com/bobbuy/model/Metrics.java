package com.bobbuy.model;

public class Metrics {
  private int users;
  private int trips;
  private int orders;
  private double gmV;
  private java.util.Map<String, Integer> orderStatusCounts;

  public Metrics(int users, int trips, int orders, double gmV, java.util.Map<String, Integer> orderStatusCounts) {
    this.users = users;
    this.trips = trips;
    this.orders = orders;
    this.gmV = gmV;
    this.orderStatusCounts = orderStatusCounts;
  }

  public int getUsers() {
    return users;
  }

  public int getTrips() {
    return trips;
  }

  public int getOrders() {
    return orders;
  }

  public double getGmV() {
    return gmV;
  }

  public java.util.Map<String, Integer> getOrderStatusCounts() {
    return orderStatusCounts;
  }
}
