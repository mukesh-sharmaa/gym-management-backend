package com.gymapp.gym_management.dto;

import java.time.LocalDate;

public class RenewRequest {
    private LocalDate newEndDate;
    private Long planId;

    public LocalDate getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(LocalDate newEndDate) {
        this.newEndDate = newEndDate;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }
}