package com.gymapp.gym_management.controller;

import com.gymapp.gym_management.entity.Plan;
import com.gymapp.gym_management.repository.PlanRepository;
import com.gymapp.gym_management.service.UserService;
import com.gymapp.gym_management.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository planRepository;
    private final UserService userService;

    public PlanController(PlanRepository planRepository, UserService userService) {
        this.planRepository = planRepository;
        this.userService = userService;
    }

    // ✅ 1. Create new plan
    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody Plan plan) {
        if (plan.getPlanName() == null || plan.getPlanName().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Plan name is required");
        }
        if (plan.getDurationInMonths() <= 0) {
            return ResponseEntity.badRequest().body("❌ Duration must be greater than 0");
        }
        if (plan.getPrice() <= 0) {
            return ResponseEntity.badRequest().body("❌ Price must be greater than 0");
        }

        // Set the current user's ID
        Long userId = userService.getCurrentUserId();
        plan.setUserId(userId);

        Plan savedPlan = planRepository.save(plan);
        return ResponseEntity.ok("✅ Plan created successfully with ID: " + savedPlan.getId());
    }

    // ✅ 2. Get all plans
    @GetMapping
    public ResponseEntity<?> getAllPlans() {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(planRepository.findByUserId(userId));
    }

    // ✅ 3. Get single plan by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable("id") Long id) {
        Long userId = userService.getCurrentUserId();
        Plan plan = planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        return ResponseEntity.ok(plan);
    }


    // ✅ 4. Update existing plan
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody Plan updatedPlan) {
        Long userId = userService.getCurrentUserId();
        Plan plan = planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));

        if (updatedPlan.getPlanName() != null && !updatedPlan.getPlanName().isEmpty()) {
            plan.setPlanName(updatedPlan.getPlanName());
        }
        if (updatedPlan.getDurationInMonths() > 0) {
            plan.setDurationInMonths(updatedPlan.getDurationInMonths());
        }
        if (updatedPlan.getPrice() > 0) {
            plan.setPrice(updatedPlan.getPrice());
        }

        planRepository.save(plan);
        return ResponseEntity.ok("✅ Plan updated successfully");
    }

    // ✅ 5. Delete plan
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Long id) {
        Long userId = userService.getCurrentUserId();
        planRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        planRepository.deleteById(id);
        return ResponseEntity.ok("🗑️ Plan deleted successfully");
    }
}
