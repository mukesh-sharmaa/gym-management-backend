package com.gymapp.gym_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------
    // BASIC MEMBER DETAILS
    // -------------------------

    @Column(nullable = false)
    private String name;

    @Column
    private String email;  // Optional field

    @Column(nullable = false, unique = false)
    private String phone;  // Required (validation done in controller)

    @Column(nullable = false)
    private Long userId;  // Link to the User/Admin who owns this member

    // -------------------------
    // RELATIONSHIP WITH PLAN
    // -------------------------

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnoreProperties("members") // prevent infinite recursion
    private Plan plan;

    // -------------------------
    // DATES
    // -------------------------

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // -------------------------
    // CONSTRUCTORS
    // -------------------------

    public Member() {}

    public Member(String name, String email, String phone, Plan plan, LocalDate startDate) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.plan = plan;
        this.startDate = startDate;

        // auto-calc if not overridden
        if (plan != null && startDate != null) {
            this.endDate = startDate.plusMonths(plan.getDurationInMonths());
        }
    }

    // -------------------------
    // GETTERS & SETTERS
    // -------------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
