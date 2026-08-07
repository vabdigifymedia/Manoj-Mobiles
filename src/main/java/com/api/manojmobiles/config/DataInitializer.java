package com.api.manojmobiles.config;

import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.Category;
import com.api.manojmobiles.entity.Brand;
import com.api.manojmobiles.entity.Product;
import com.api.manojmobiles.entity.ProductVariant;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.entity.enums.UserStatus;
import com.api.manojmobiles.entity.enums.ProductStatus;
import com.api.manojmobiles.entity.enums.StockStatus;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.repository.CategoryRepository;
import com.api.manojmobiles.repository.BrandRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
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

        if (productRepository.count() == 0) {
            log.info("No products found. Initializing seed data for frontend testing...");
            Category mobileCat = Category.builder().name("Mobiles").build();
            categoryRepository.save(mobileCat);

            Brand appleBrand = Brand.builder().name("Apple").logo_url("apple-logo.png").build();
            Brand samsungBrand = Brand.builder().name("Samsung").logo_url("samsung-logo.png").build();
            brandRepository.save(appleBrand);
            brandRepository.save(samsungBrand);

            Product p1 = Product.builder()
                    .name("iPhone 15 Pro")
                    .description("Latest Apple iPhone 15 Pro")
                    .category(mobileCat)
                    .brand(appleBrand)
                    .status(ProductStatus.ACTIVE)
                    .build();
            productRepository.save(p1);

            ProductVariant p1v1 = ProductVariant.builder()
                    .product(p1)
                    .variantName("256GB Titanium")
                    .sku("IP15PRO-256-TIT")
                    .mrp(new BigDecimal("134900.00"))
                    .sellingPrice(new BigDecimal("129900.00"))
                    .stockQty(50)
                    .stockStatus(StockStatus.IN_STOCK)
                    .build();
            productVariantRepository.save(p1v1);

            Product p2 = Product.builder()
                    .name("Samsung Galaxy S24 Ultra")
                    .description("Samsung's flagship AI phone")
                    .category(mobileCat)
                    .brand(samsungBrand)
                    .status(ProductStatus.ACTIVE)
                    .build();
            productRepository.save(p2);

            ProductVariant p2v1 = ProductVariant.builder()
                    .product(p2)
                    .variantName("512GB Phantom Black")
                    .sku("S24U-512-BLK")
                    .mrp(new BigDecimal("139999.00"))
                    .sellingPrice(new BigDecimal("129999.00"))
                    .stockQty(30)
                    .stockStatus(StockStatus.IN_STOCK)
                    .build();
            productVariantRepository.save(p2v1);

            log.info("Seed data initialized successfully!");
        }
    }
}
