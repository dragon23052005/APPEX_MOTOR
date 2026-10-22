package com.apexmotors.persistence.repository;

import com.apexmotors.persistence.entity.FavoriteEntity;
import com.apexmotors.persistence.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Transactional(readOnly = true)
public interface FavoriteJpaRepository extends JpaRepository<FavoriteEntity, FavoriteId> {
    List<FavoriteEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
