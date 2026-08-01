package br.com.corely.comercial.studentplan.dto;

import br.com.corely.comercial.billingschedule.BillingFrequency;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.SuspensionReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta completa de uma matricula de aluno")
public class StudentPlanResponse {

    @Schema(description = "ID da matricula")
    private UUID id;

    @Schema(description = "ID do aluno")
    private UUID studentId;

    @Schema(description = "Nome do aluno")
    private String studentName;

    @Schema(description = "ID do snapshot contratual")
    private UUID contractSnapshotId;

    @Schema(description = "Nome do plano (snapshot)")
    private String snapshotName;

    @Schema(description = "ID do plano (origem: ContractSnapshot.planId)")
    private UUID planId;

    @Schema(description = "Descricao do plano (origem: ContractSnapshot.planDescription). Pode ser null quando o plano nao possui descricao")
    private String planDescription;

    @Schema(description = "Preco do plano (origem: ContractSnapshot.planPrice)")
    private BigDecimal planPrice;

    @Schema(description = "Ciclo de cobranca (origem: BillingSchedule.frequency). Null quando nao ha billing schedule ativo")
    private BillingFrequency billingCycle;

    @Schema(description = "Quantidade de aulas por semana (origem: regra WEEKLY_CLASSES do ContractSnapshot.rules). Null quando a regra nao esta definida")
    private Integer weeklyClasses;

    @Schema(description = "Status da matricula")
    private StudentPlanStatus status;

    @Schema(description = "Data de inicio")
    private LocalDate startDate;

    @Schema(description = "Data de termino")
    private LocalDate endDate;

    @Schema(description = "Data da proxima cobranca (origem: BillingSchedule.nextBillingDate). Null quando nao ha billing schedule ativo")
    private LocalDate nextBillingDate;

    @Schema(description = "Frequencia de cobranca (origem: BillingSchedule.frequency). Null quando nao ha billing schedule ativo")
    private BillingFrequency nextBillingFrequency;

    @Schema(description = "Dia da cobranca (origem: BillingSchedule.billingDay). Null quando nao ha billing schedule ativo")
    private Integer nextBillingDay;

    @Schema(description = "Se a cobranca esta ativa (origem: BillingSchedule.active). Null quando nao ha billing schedule ativo")
    private Boolean nextBillingActive;

    @Schema(description = "Data de criacao")
    private LocalDateTime createdAt;

    @Schema(description = "Data da ultima atualizacao")
    private LocalDateTime updatedAt;

    @Schema(description = "Data de cancelamento")
    private LocalDate cancellationDate;

    @Schema(description = "Motivo do cancelamento")
    private String cancellationReason;

    @Schema(description = "Se o agendamento esta bloqueado")
    private Boolean bookingBlocked;

    @Schema(description = "Motivo da suspensao")
    private SuspensionReason suspensionReason;

    public StudentPlanResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public UUID getContractSnapshotId() { return contractSnapshotId; }
    public void setContractSnapshotId(UUID contractSnapshotId) { this.contractSnapshotId = contractSnapshotId; }
    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public String getPlanDescription() { return planDescription; }
    public void setPlanDescription(String planDescription) { this.planDescription = planDescription; }
    public BigDecimal getPlanPrice() { return planPrice; }
    public void setPlanPrice(BigDecimal planPrice) { this.planPrice = planPrice; }
    public BillingFrequency getBillingCycle() { return billingCycle; }
    public void setBillingCycle(BillingFrequency billingCycle) { this.billingCycle = billingCycle; }
    public Integer getWeeklyClasses() { return weeklyClasses; }
    public void setWeeklyClasses(Integer weeklyClasses) { this.weeklyClasses = weeklyClasses; }
    public StudentPlanStatus getStatus() { return status; }
    public void setStatus(StudentPlanStatus status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(LocalDate nextBillingDate) { this.nextBillingDate = nextBillingDate; }
    public BillingFrequency getNextBillingFrequency() { return nextBillingFrequency; }
    public void setNextBillingFrequency(BillingFrequency nextBillingFrequency) { this.nextBillingFrequency = nextBillingFrequency; }
    public Integer getNextBillingDay() { return nextBillingDay; }
    public void setNextBillingDay(Integer nextBillingDay) { this.nextBillingDay = nextBillingDay; }
    public Boolean getNextBillingActive() { return nextBillingActive; }
    public void setNextBillingActive(Boolean nextBillingActive) { this.nextBillingActive = nextBillingActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDate getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(LocalDate cancellationDate) { this.cancellationDate = cancellationDate; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Boolean getBookingBlocked() { return bookingBlocked; }
    public void setBookingBlocked(Boolean bookingBlocked) { this.bookingBlocked = bookingBlocked; }
    public SuspensionReason getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(SuspensionReason suspensionReason) { this.suspensionReason = suspensionReason; }
}
