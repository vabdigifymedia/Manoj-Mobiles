package com.api.manojmobiles.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// =========================================================================
// [TEMPORARY - PINE LABS APPROVAL]
// Reason: TD Digital SMS credits exhausted; added for PG review team.
// Removal: Delete this class and corresponding endpoints once SMS is restored.
// =========================================================================
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailOtpRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
