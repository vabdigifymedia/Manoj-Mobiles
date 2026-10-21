package com.api.manojmobiles.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CustomerEmailSignUpDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a valid 10-digit number")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit number")
    private String otp;
}
