package br.com.corely.classsession.dto;

import br.com.corely.classsession.CancelReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelSessionRequest {
    @NotNull(message = "Motivo do cancelamento é obrigatório")
    private CancelReason cancelReason;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String cancelDescription;
}
