package com.api.manojmobiles.dto.auth;

import com.api.manojmobiles.entity.enums.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
}
