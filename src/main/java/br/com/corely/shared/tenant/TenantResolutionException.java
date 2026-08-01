package br.com.corely.shared.tenant;

/**
 * Exceção lançada quando o tenant corrente não pode ser resolvido a partir do
 * contexto de autenticação.
 */
public class TenantResolutionException extends RuntimeException {

    public TenantResolutionException(String message) {
        super(message);
    }
}
