package com.sbpl.OPD.Auth.config;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Data initializer for Healthcare Management System.
 * Sets up default admin users and initial system configuration.
 *
 * @author Rahul Kumar
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    // Removed PermissionService dependency for pure RBAC
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing HMS data...");
        initializeDefaultUsers();
        log.info("HMS data initialization completed");
    }

    // Removed permission initialization for pure RBAC

    private void initializeDefaultUsers() {
        try {
            log.info("Initializing default users...");

            // Create Super Admin only - other users will be added hierarchically
            createDefaultUser("superadmin", "superadmin@hospital.com", "SuperAdmin123",
                    UserRole.SUPER_ADMIN, "System", "SuperAdmin");

            log.info("Default superadmin initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing default users: {}", e.getMessage(), e);
        }
    }

    private void createDefaultUser(String username, String email, String password,
                                   UserRole role, String firstName, String lastName) {
        try {
            // Check if user already exists
            if (userRepository.findByUsername(username).isPresent()) {
                log.info("User {} already exists, skipping creation", username);
                return;
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setIsActive(true);
            user.setPhoneNumber("9999999999"); // Default phone number

            userRepository.save(user);
            log.info("Created default user: {} with role {}", username, role);
        } catch (Exception e) {
            log.error("Error creating default user {}: {}", username, e.getMessage());
        }
    }


}