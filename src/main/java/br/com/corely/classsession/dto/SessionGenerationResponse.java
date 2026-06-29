package br.com.corely.classsession.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionGenerationResponse {
    private int created;
    private int ignored;
}
