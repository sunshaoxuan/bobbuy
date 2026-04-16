package com.bobbuy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "bb_experience_mapping", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bb_experience_mapping_fuzzy_term", columnNames = "fuzzy_term")
})
public class ExperienceMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "fuzzy_term", nullable = false, length = 255)
  private String fuzzyTerm;

  @Column(name = "mapped_name", nullable = false, length = 255)
  private String mappedName;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ExperienceMapping() {
  }

  public ExperienceMapping(String fuzzyTerm, String mappedName) {
    this.fuzzyTerm = fuzzyTerm;
    this.mappedName = mappedName;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFuzzyTerm() {
    return fuzzyTerm;
  }

  public void setFuzzyTerm(String fuzzyTerm) {
    this.fuzzyTerm = fuzzyTerm;
  }

  public String getMappedName() {
    return mappedName;
  }

  public void setMappedName(String mappedName) {
    this.mappedName = mappedName;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
