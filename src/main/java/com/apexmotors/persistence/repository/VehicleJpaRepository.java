package com.apexmotors.persistence.repository;

import com.apexmotors.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Transactional(readOnly = true)
public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {
    @EntityGraph(attributePaths = "brand")
    Optional<VehicleEntity> findBySlug(String slug);

    @EntityGraph(attributePaths = "brand")
    List<VehicleEntity> findAllByOrderByIdAsc();
}
