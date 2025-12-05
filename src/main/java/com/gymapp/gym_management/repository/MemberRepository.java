package com.gymapp.gym_management.repository;

import com.gymapp.gym_management.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByEndDateBetween(LocalDate today, LocalDate upcoming);
    
    // User-specific queries
    List<Member> findByUserId(Long userId);
    List<Member> findByUserIdAndEndDateBetween(Long userId, LocalDate today, LocalDate upcoming);
    Optional<Member> findByIdAndUserId(Long id, Long userId);
}