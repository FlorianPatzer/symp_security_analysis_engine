package de.fraunhofer.iosb.svs.sae.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    boolean existsByUuid(String uuid);
}
