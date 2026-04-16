package com.bobbuy.repository;

import com.bobbuy.model.ExperienceMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExperienceMappingRepository extends JpaRepository<ExperienceMapping, Long> {
  Optional<ExperienceMapping> findByFuzzyTerm(String fuzzyTerm);
}
