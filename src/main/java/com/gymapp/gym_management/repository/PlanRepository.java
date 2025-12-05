package com.gymapp.gym_management.repository;

import com.gymapp.gym_management.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    // User-specific queries
    List<Plan> findByUserId(Long userId);
    Optional<Plan> findByIdAndUserId(Long id, Long userId);
}
