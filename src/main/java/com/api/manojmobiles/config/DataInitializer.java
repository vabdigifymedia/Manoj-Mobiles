package com.api.manojmobiles.config;

import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.entity.enums.UserStatus;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.name:Super Admin}")
    private String adminName;

    @Value("${app.initial-admin.email:admin@manojmobiles.com}")
    private String adminEmail;

    @Value("${app.initial-admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.initial-admin.phone:9999999999}")
    private String adminPhone;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            log.info("No Admin found in database. Initializing default Super Admin...");
            User admin = User.builder()
                    .name(adminName)
                    .email(adminEmail)
                    .phone(adminPhone)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            log.info("Super Admin created successfully! Email: {} | Password: {}", adminEmail, adminPassword);
        } else {
            log.info("Admin account already exists. Skipping initial admin creation.");
        }
    }
}

