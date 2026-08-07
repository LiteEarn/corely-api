package br.com.corely.finance.cash;

import br.com.corely.finance.cash.dto.CashPaymentRequest;
import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de pagamentos em dinheiro (EPIC-03-S09).
 *
 * <p>Registra a liquidação de um recebível (ou parcela) em dinheiro e permite a
 * consulta dos pagamentos em dinheiro do estúdio corrente. Reutiliza o
 * {@link PaymentService} para a baixa (método {@code CASH}), garantindo as
 * mesmas regras de negócio (recebível em aberto, sem pagamento prévio, valor
 * igual) e o registro da movimentação no histórico.</p>
 */
@Service("financeCashPaymentService")
@RequiredArgsConstructor
public class CashPaymentService {

    private final PaymentService paymentService;

    /**
     * Registra a baixa manual de um recebível (ou parcela) em dinheiro.
     *
     * @param request dados da baixa em dinheiro
     * @return pagamento registrado
     */
    @Transactional
    public PaymentResponse create(CashPaymentRequest request) {
        var paymentRequest = new PaymentRequest();
        paymentRequest.setReceivableId(request.getReceivableId());
        paymentRequest.setInstallmentId(request.getInstallmentId());
        paymentRequest.setPaymentDate(request.getPaymentDate());
        paymentRequest.setAmount(request.getAmount());
        paymentRequest.setPaymentMethod(PaymentMethodDto.CASH);
        paymentRequest.setNotes(request.getNotes());
        return paymentService.create(paymentRequest);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(Pageable pageable) {
        return paymentService.findAllByMethod(PaymentMethodDto.CASH, pageable);
    }
}