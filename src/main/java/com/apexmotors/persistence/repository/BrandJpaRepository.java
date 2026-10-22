package com.apexmotors.persistence.repository;

import com.apexmotors.persistence.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Transactional(readOnly = true)
public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {
    Optional<BrandEntity> findBySlug(String slug);
}
