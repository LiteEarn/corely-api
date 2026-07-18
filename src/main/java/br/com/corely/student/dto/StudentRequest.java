package br.com.corely.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("deprecation")

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private LocalDate birthDate;

    private Boolean active;

    private Boolean billingEnabled;

    @NotNull(message = "Membership plan is required")
    private UUID membershipPlanId;
}
