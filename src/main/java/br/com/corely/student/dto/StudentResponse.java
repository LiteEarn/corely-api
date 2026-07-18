package br.com.corely.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private UUID id;
    private UUID studioId;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private Boolean active;

    private Boolean billingEnabled;

    private UUID membershipPlanId;
    private String membershipPlanName;
}
