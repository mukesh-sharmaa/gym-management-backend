package com.gymapp.gym_management.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email"}),
                @UniqueConstraint(columnNames = {"phone"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", initialValue = 10001, allocationSize = 1)
    private Long id;

    private String adminName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String role = "ADMIN";

    private String gymName;

    private String gymAddress;

    private String gymContactNumber;

    public User() {}

    public User(String adminName, String email, String phone, String password, String gymName, String gymAddress, String gymContactNumber) {
        this.adminName = adminName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.gymName = gymName;
        this.gymAddress = gymAddress;
        this.gymContactNumber = gymContactNumber;
    }

    // ✅ Getters & Setters
    public Long getId() { return id; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getGymName() { return gymName; }
    public void setGymName(String gymName) { this.gymName = gymName; }

    public String getGymAddress() { return gymAddress; }
    public void setGymAddress(String gymAddress) { this.gymAddress = gymAddress; }

    public String getGymContactNumber() { return gymContactNumber; }
    public void setGymContactNumber(String gymContactNumber) { this.gymContactNumber = gymContactNumber; }
}
