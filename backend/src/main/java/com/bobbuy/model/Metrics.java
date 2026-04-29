package com.bobbuy.model;

public class Metrics {
  private int users;
  private int trips;
  private int orders;
  private double gmV;
  private java.util.Map<String, Integer> orderStatusCounts;
  private java.util.Map<String, Long> requestCounts;
  private java.util.Map<String, Long> statusCounts;
  private java.util.Map<String, Long> latencyP95Ms;
  private java.util.Map<String, Long> latencyP99Ms;
  private java.util.Map<String, Long> http4xxByEndpoint;
  private java.util.Map<String, Long> http5xxByEndpoint;
  private long loginFailureCount;
  private double overall5xxRate;
  private java.util.List<String> slowEndpoints;

  public Metrics(int users, int trips, int orders, double gmV,
                 java.util.Map<String, Integer> orderStatusCounts,
                 java.util.Map<String, Long> requestCounts,
                 java.util.Map<String, Long> statusCounts,
                 java.util.Map<String, Long> latencyP95Ms,
                 java.util.Map<String, Long> latencyP99Ms,
                 java.util.Map<String, Long> http4xxByEndpoint,
                 java.util.Map<String, Long> http5xxByEndpoint,
                 long loginFailureCount,
                 double overall5xxRate,
                 java.util.List<String> slowEndpoints) {
    this.users = users;
    this.trips = trips;
    this.orders = orders;
    this.gmV = gmV;
    this.orderStatusCounts = orderStatusCounts;
    this.requestCounts = requestCounts;
    this.statusCounts = statusCounts;
    this.latencyP95Ms = latencyP95Ms;
    this.latencyP99Ms = latencyP99Ms;
    this.http4xxByEndpoint = http4xxByEndpoint;
    this.http5xxByEndpoint = http5xxByEndpoint;
    this.loginFailureCount = loginFailureCount;
    this.overall5xxRate = overall5xxRate;
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

  public java.util.Map<String, Long> getRequestCounts() {
    return requestCounts;
  }

  public java.util.Map<String, Long> getStatusCounts() {
    return statusCounts;
  }

  public java.util.Map<String, Long> getLatencyP95Ms() {
    return latencyP95Ms;
  }

  public java.util.Map<String, Long> getLatencyP99Ms() {
    return latencyP99Ms;
  }

  public java.util.Map<String, Long> getHttp4xxByEndpoint() {
    return http4xxByEndpoint;
  }

  public java.util.Map<String, Long> getHttp5xxByEndpoint() {
    return http5xxByEndpoint;
  }

  public long getLoginFailureCount() {
    return loginFailureCount;
  }

  public double getOverall5xxRate() {
    return overall5xxRate;
  }

  public java.util.List<String> getSlowEndpoints() {
    return slowEndpoints;
  }
}
