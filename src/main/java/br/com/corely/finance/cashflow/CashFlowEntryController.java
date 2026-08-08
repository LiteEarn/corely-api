package br.com.corely.finance.cashflow;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.cashflow.dto.CashFlowBalanceResponse;
import br.com.corely.finance.cashflow.dto.CashFlowEntryRequest;
import br.com.corely.finance.cashflow.dto.CashFlowEntryResponse;
import br.com.corely.finance.cashflow.dto.CashFlowEntryTypeDto;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints de fluxo de caixa — entradas e saídas (EPIC-03-S11/S12).
 */
@RestController("financeCashFlowEntryController")
@RequestMapping("/finance/cash-flow/entries")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Fluxo de Caixa — Entradas e Saídas")
public class CashFlowEntryController {

    private final CashFlowEntryService cashFlowEntryService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Registrar entrada de caixa",
            description = "Registra uma entrada (ou saída) no fluxo de caixa do estúdio corrente.")
    public ResponseEntity<CashFlowEntryResponse> create(@Valid @RequestBody CashFlowEntryRequest request) {
        var response = cashFlowEntryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar movimentos de caixa",
            description = "Lista os movimentos de caixa do estúdio corrente (paginado, por data decrescente), com filtros opcionais de tipo e período.")
    public ResponseEntity<Page<CashFlowEntryResponse>> findAll(
            @RequestParam(required = false) CashFlowEntryTypeDto entryType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {
        return ResponseEntity.ok(cashFlowEntryService.findAll(entryType, dateFrom, dateTo, pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar movimento de caixa por ID")
    public ResponseEntity<CashFlowEntryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(cashFlowEntryService.findById(id));
    }

    @GetMapping("/balance")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Calcular saldo de caixa",
            description = "Calcula o saldo de caixa do estúdio corrente (entradas − saídas), com filtros opcionais de período.")
    public ResponseEntity<CashFlowBalanceResponse> getBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(cashFlowEntryService.getBalance(dateFrom, dateTo));
    }
}
