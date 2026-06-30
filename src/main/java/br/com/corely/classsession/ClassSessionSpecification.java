package br.com.corely.classsession;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public class ClassSessionSpecification {

    public static Specification<ClassSession> withFilters(UUID classGroupId, UUID instructorId, 
                                                           ClassSessionStatus status, LocalDate sessionDate) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (classGroupId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("classGroup").get("id"), classGroupId));
            }

            if (instructorId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("instructor").get("id"), instructorId));
            }

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (sessionDate != null) {
                predicate = cb.and(predicate, cb.equal(root.get("sessionDate"), sessionDate));
            }

            return predicate;
        };
    }
}
