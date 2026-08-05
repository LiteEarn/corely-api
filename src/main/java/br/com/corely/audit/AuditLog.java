package br.com.corely.audit;

import br.com.corely.studio.Studio;
import br.com.corely.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro da trilha de auditoria (EPIC-02-S09).
 *
 * <p>Imutável por natureza: um log é criado uma única vez e nunca alterado.
 * Armazena quem (usuário), onde (estúdio), quando ({@link #occurredAt}), de
 * onde (IP) e o que foi feito (evento + recurso + detalhes).</p>
 *
 * <p>Aplicado o filtro de tenant Hibernate ({@code comercialTenantFilter}) para
 * que consultas base do repositório (ex.: {@code findAll()}) também respeitem o
 * isolamento por estúdio — além da consulta explícita por {@code studioId}.</p>
 */
@Entity
@Table(name = "audit_logs")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private AuditEvent action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
