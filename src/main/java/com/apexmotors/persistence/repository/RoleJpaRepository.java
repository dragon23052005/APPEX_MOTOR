package com.apexmotors.persistence.repository;

import com.apexmotors.persistence.entity.RoleEntity;
import com.apexmotors.persistence.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Transactional(readOnly = true)
public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(RoleName name);
}
