package com.gymapp.gym_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "plans")
@JsonIgnoreProperties({"members"})  // prevent recursion when returning JSON
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private int durationInMonths;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Long userId;  // Link to the User/Admin who owns this plan

    // Reverse relationship (Plan → Members)
    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<Member> members;

    // --------------------------
    // Constructors
    // --------------------------

    public Plan() {}

    public Plan(String planName, int durationInMonths, double price) {
        this.planName = planName;
        this.durationInMonths = durationInMonths;
        this.price = price;
    }

    // --------------------------
    // Getters & Setters
    // --------------------------

    public Long getId() {
        return id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public int getDurationInMonths() {
        return durationInMonths;
    }

    public void setDurationInMonths(int durationInMonths) {
        this.durationInMonths = durationInMonths;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
