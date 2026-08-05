package br.com.corely.audit;

/**
 * Eventos auditáveis na trilha de auditoria (EPIC-02-S09).
 *
 * <p>Eventos relevantes para conformidade LGPD: autenticação, autorização e
 * operações sensíveis. Cada evento registra quem (usuário), quando, de onde
 * (IP) e o que foi feito.</p>
 */
public enum AuditEvent {

    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    TOKEN_REFRESH,
    LOCKOUT_TRIGGERED
}
