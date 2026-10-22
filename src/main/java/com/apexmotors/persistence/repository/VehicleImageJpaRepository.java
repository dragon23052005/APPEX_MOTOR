package com.apexmotors.persistence.repository;

import com.apexmotors.persistence.entity.VehicleImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Transactional(readOnly = true)
public interface VehicleImageJpaRepository extends JpaRepository<VehicleImageEntity, Long> {
    List<VehicleImageEntity> findByVehicleIdOrderByDisplayOrderAsc(Long vehicleId);
}
