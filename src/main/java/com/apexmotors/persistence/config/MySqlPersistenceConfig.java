package com.apexmotors.persistence.config;

import com.apexmotors.persistence.entity.VehicleEntity;
import com.apexmotors.persistence.repository.VehicleJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("mysql")
@EntityScan(basePackageClasses = VehicleEntity.class)
@EnableJpaRepositories(basePackageClasses = VehicleJpaRepository.class)
public class MySqlPersistenceConfig {
}
