package sk.kasv.fx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sk.kasv.fx.entity.AuditEntry;

public interface AuditRepository extends JpaRepository<AuditEntry, Long>, JpaSpecificationExecutor<AuditEntry> {}
