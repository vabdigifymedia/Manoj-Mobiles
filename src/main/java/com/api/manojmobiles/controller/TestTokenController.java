package com.api.manojmobiles.controller;

import com.api.manojmobiles.entity.User;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.repository.UserRepository;
import com.api.manojmobiles.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping("/token")
    @Transactional
    public String getToken(@RequestParam String roleStr) {
        Role role = Role.valueOf(roleStr);
        String phone = "999999999" + (role == Role.ADMIN ? "1" : "2");
        String email = roleStr.toLowerCase() + "@test.com";
        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User u = new User();
            u.setPhone(phone);
            u.setEmail(email);
            u.setName(roleStr);
            u.setRole(role);
            u.setStatus(com.api.manojmobiles.entity.enums.UserStatus.ACTIVE);
            u.setCreatedAt(java.time.LocalDateTime.now());
            u.setUpdatedAt(java.time.LocalDateTime.now());
            return userRepository.save(u);
        });
        return jwtTokenProvider.generateToken(role == Role.ADMIN ? email : phone);
    }
}
