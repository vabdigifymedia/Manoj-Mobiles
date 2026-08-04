package com.api.manojmobiles.dto.user;

import com.api.manojmobiles.entity.enums.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String status;
    private LocalDateTime createdAt;
}