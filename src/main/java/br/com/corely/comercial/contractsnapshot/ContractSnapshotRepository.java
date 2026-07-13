package br.com.corely.comercial.contractsnapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractSnapshotRepository extends JpaRepository<ContractSnapshot, UUID> {

    List<ContractSnapshot> findByPlanIdOrderByCreatedAtDesc(UUID planId);
}
