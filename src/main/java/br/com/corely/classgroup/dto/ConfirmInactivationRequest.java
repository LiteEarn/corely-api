package br.com.corely.classgroup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmInactivationRequest {
    private boolean cascadeEnrollments;
}
