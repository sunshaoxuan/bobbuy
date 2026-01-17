package com.bobbuy.model;

public class Metrics {
  private int users;
  private int trips;
  private int orders;
  private double gmV;
  private java.util.Map<String, Integer> orderStatusCounts;
  private java.util.Map<String, Long> latencyP95Ms;
  private java.util.Map<String, Long> latencyP99Ms;
  private java.util.List<String> slowEndpoints;

  public Metrics(int users, int trips, int orders, double gmV,
                 java.util.Map<String, Integer> orderStatusCounts,
                 java.util.Map<String, Long> latencyP95Ms,
                 java.util.Map<String, Long> latencyP99Ms,
                 java.util.List<String> slowEndpoints) {
    this.users = users;
    this.trips = trips;
    this.orders = orders;
    this.gmV = gmV;
    this.orderStatusCounts = orderStatusCounts;
    this.latencyP95Ms = latencyP95Ms;
    this.latencyP99Ms = latencyP99Ms;
    this.slowEndpoints = slowEndpoints;
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

  public java.util.Map<String, Long> getLatencyP95Ms() {
    return latencyP95Ms;
  }

  public java.util.Map<String, Long> getLatencyP99Ms() {
    return latencyP99Ms;
  }

  public java.util.List<String> getSlowEndpoints() {
    return slowEndpoints;
  }
}
