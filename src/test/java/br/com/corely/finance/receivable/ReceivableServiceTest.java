package br.com.corely.finance.receivable;

import br.com.corely.finance.receivable.dto.ReceivableRequest;
import br.com.corely.finance.receivable.dto.ReceivableResponse;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de recebíveis (EPIC-03-S01).
 */
@ExtendWith(MockitoExtension.class)
class ReceivableServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private TenantContext tenantContext;

    private ReceivableService service;

    private UUID studioId;
    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new ReceivableService(receivableRepository, studentRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
        studio.setName("Test Studio");

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setStudio(studio);
        student.setFullName("Student One");
    }

    @Test
    void create_shouldPersistReceivableWithOpenStatus() {
        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setDescription("Mensalidade");
        request.setAmount(BigDecimal.valueOf(199.90));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));

        var saved = new Receivable();
        saved.setId(UUID.randomUUID());
        saved.setStudio(studio);
        saved.setStudent(student);
        saved.setDescription("Mensalidade");
        saved.setAmount(BigDecimal.valueOf(199.90));
        saved.setDueDate(LocalDate.of(2026, 9, 10));
        saved.setStatus(ReceivableStatus.OPEN);
        when(receivableRepository.save(any(Receivable.class))).thenReturn(saved);

        ReceivableResponse response = service.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getAmount()).isEqualByComparingTo("199.90");
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(response.getStatus()).isEqualTo(ReceivableStatus.OPEN);
    }

    @Test
    void create_shouldThrowWhenStudentNotFound() {
        var request = new ReceivableRequest();
        request.setStudentId(UUID.randomUUID());
        request.setAmount(BigDecimal.TEN);
        request.setDueDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_shouldReturnReceivable() {
        UUID id = UUID.randomUUID();
        var receivable = new Receivable();
        receivable.setId(id);
        receivable.setStudio(studio);
        receivable.setStudent(student);
        receivable.setAmount(BigDecimal.valueOf(99));
        receivable.setDueDate(LocalDate.now());
        receivable.setStatus(ReceivableStatus.OPEN);
        when(receivableRepository.findById(id)).thenReturn(Optional.of(receivable));

        ReceivableResponse response = service.findById(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStudentName()).isEqualTo("Student One");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(receivableRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldDelegateFiltersAndReturnPage() {
        UUID studentId = student.getId();
        var receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setStudio(studio);
        receivable.setStudent(student);
        receivable.setAmount(BigDecimal.valueOf(50));
        receivable.setDueDate(LocalDate.now());
        receivable.setStatus(ReceivableStatus.OPEN);

        var page = new PageImpl<>(List.of(receivable), PageRequest.of(0, 10), 1);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findByFilters(eq(studioId), eq(ReceivableStatus.OPEN), eq(studentId),
                any(), any(), any(PageRequest.class))).thenReturn(page);

        Page<ReceivableResponse> result = service.findAll(
                ReceivableStatus.OPEN, studentId, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ReceivableStatus.OPEN);
    }
}
