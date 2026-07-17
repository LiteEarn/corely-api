package br.com.corely.comercial.makeup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("comercialMakeUpRepository")
public interface MakeUpRepository extends JpaRepository<MakeUpCredit, UUID> {

    @Query("SELECT m FROM ComercialMakeUpCredit m WHERE m.student.id = :studentId")
    Page<MakeUpCredit> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    @Query("SELECT m FROM ComercialMakeUpCredit m WHERE m.status = :status")
    Page<MakeUpCredit> findByStatus(@Param("status") MakeUpCreditStatus status, Pageable pageable);

    @Override
    @Query("SELECT m FROM ComercialMakeUpCredit m WHERE m.id = :id")
    Optional<MakeUpCredit> findById(@Param("id") UUID id);

    @Query("SELECT m.originalClassSession.id, COUNT(m) FROM ComercialMakeUpCredit m WHERE m.originalClassSession.id IN :sessionIds GROUP BY m.originalClassSession.id")
    List<Object[]> countByOriginalSessionIds(@Param("sessionIds") List<UUID> sessionIds);
}
