package com.bobbuy.model;

public class Metrics {
  private int users;
  private int trips;
  private int orders;
  private double gmV;

  public Metrics(int users, int trips, int orders, double gmV) {
    this.users = users;
    this.trips = trips;
    this.orders = orders;
    this.gmV = gmV;
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
}
