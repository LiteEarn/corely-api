package br.com.corely.comercial.booking;

import br.com.corely.comercial.classsession.ClassSessionCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClassSessionCancelledListener {

    private final BookingRepository bookingRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onClassSessionCancelled(ClassSessionCancelledEvent event) {
        var bookings = bookingRepository.findByClassSessionIdAndStatus(
                event.classSessionId(), BookingStatus.CONFIRMED);

        // TODO: decisão de produto sobre gerar makeup credit em cancelamento de sessão
        for (var booking : bookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setActive(false);
            booking.setCancelReason(CancelReason.SESSION_CANCELLED);
            booking.setCancelDescription("Sessão cancelada");
            // cancelledBy fica null: a ação não foi de um usuário específico,
            // foi decorrência do cancelamento da sessão
            booking.setCancelledAt(LocalDateTime.now());
            // NÃO decrementar bookedCount da ClassSession: a sessão inteira já está CANCELLED
            // NÃO publicar BookingCancelledEvent para evitar geração automática de makeup credits
        }

        bookingRepository.saveAll(bookings);
    }
}
