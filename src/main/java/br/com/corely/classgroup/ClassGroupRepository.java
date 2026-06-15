package br.com.corely.classgroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, UUID> {
}
