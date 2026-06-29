package br.com.corely.makeup.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MakeupRequestRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
