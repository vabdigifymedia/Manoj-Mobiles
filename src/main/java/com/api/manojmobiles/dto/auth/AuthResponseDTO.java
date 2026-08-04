package com.api.manojmobiles.dto.auth;

import com.api.manojmobiles.entity.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
    private Boolean mustChangePassword;
}
