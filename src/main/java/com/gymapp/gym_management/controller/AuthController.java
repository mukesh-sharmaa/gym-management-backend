package com.gymapp.gym_management.controller;

import com.gymapp.gym_management.entity.User;
import com.gymapp.gym_management.repository.UserRepository;
import com.gymapp.gym_management.security.JwtUtil;
import com.gymapp.gym_management.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, UserService userService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    // ✅ Signup
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        // ✅ Validation for unique email/phone
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already exists");
        }

        // ✅ Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // ✅ Default role
        user.setRole("ADMIN");

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }


    // ✅ Login using either email or phone
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> dbUser = request.getEmailOrPhone().contains("@") ?
                userRepository.findByEmail(request.getEmailOrPhone()) :
                userRepository.findByPhone(request.getEmailOrPhone());

        if (dbUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid email/phone");
        }

        if (!passwordEncoder.matches(request.getPassword(), dbUser.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid password");
        }

        String token = jwtUtil.generateToken(dbUser.get().getEmail());
        return ResponseEntity.ok(new LoginResponse("Login successful", dbUser.get().getAdminName(), token));
    }

    // ✅ Get current user profile
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            User user = userService.getCurrentUser();
            UserProfileResponse response = new UserProfileResponse(
                    user.getId(),
                    user.getAdminName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getGymName(),
                    user.getGymAddress(),
                    user.getGymContactNumber(),
                    user.getRole()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
    }

    // ✅ Update user profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        try {
            User user = userService.getCurrentUser();

            // Update fields if provided
            if (request.getAdminName() != null) {
                user.setAdminName(request.getAdminName());
            }
            if (request.getPhone() != null) {
                // Check if phone is already taken by another user
                Optional<User> existingPhone = userRepository.findByPhone(request.getPhone());
                if (existingPhone.isPresent() && !existingPhone.get().getId().equals(user.getId())) {
                    return ResponseEntity.badRequest().body("Phone already exists");
                }
                user.setPhone(request.getPhone());
            }
            if (request.getGymName() != null) {
                user.setGymName(request.getGymName());
            }
            if (request.getGymAddress() != null) {
                user.setGymAddress(request.getGymAddress());
            }
            if (request.getGymContactNumber() != null) {
                user.setGymContactNumber(request.getGymContactNumber());
            }
            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            userRepository.save(user);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
    }

    // Inner DTOs
    static class LoginRequest {
        private String emailOrPhone;
        private String password;

        public String getEmailOrPhone() { return emailOrPhone; }
        public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class LoginResponse {
        private String message;
        private String adminName;
        private String token;

        public LoginResponse(String message, String adminName, String token) {
            this.message = message;
            this.adminName = adminName;
            this.token = token;
        }

        public String getMessage() { return message; }
        public String getAdminName() { return adminName; }
        public String getToken() { return token; }
    }

    static class UserProfileResponse {
        private Long id;
        private String adminName;
        private String email;
        private String phone;
        private String gymName;
        private String gymAddress;
        private String gymContactNumber;
        private String role;

        public UserProfileResponse(Long id, String adminName, String email, String phone, 
                                   String gymName, String gymAddress, String gymContactNumber, String role) {
            this.id = id;
            this.adminName = adminName;
            this.email = email;
            this.phone = phone;
            this.gymName = gymName;
            this.gymAddress = gymAddress;
            this.gymContactNumber = gymContactNumber;
            this.role = role;
        }

        public Long getId() { return id; }
        public String getAdminName() { return adminName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getGymName() { return gymName; }
        public String getGymAddress() { return gymAddress; }
        public String getGymContactNumber() { return gymContactNumber; }
        public String getRole() { return role; }
    }

    static class UpdateProfileRequest {
        private String adminName;
        private String phone;
        private String gymName;
        private String gymAddress;
        private String gymContactNumber;
        private String password;

        public String getAdminName() { return adminName; }
        public void setAdminName(String adminName) { this.adminName = adminName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getGymName() { return gymName; }
        public void setGymName(String gymName) { this.gymName = gymName; }
        public String getGymAddress() { return gymAddress; }
        public void setGymAddress(String gymAddress) { this.gymAddress = gymAddress; }
        public String getGymContactNumber() { return gymContactNumber; }
        public void setGymContactNumber(String gymContactNumber) { this.gymContactNumber = gymContactNumber; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
