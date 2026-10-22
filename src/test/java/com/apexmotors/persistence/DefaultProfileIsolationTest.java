package com.apexmotors.persistence;

import com.apexmotors.persistence.repository.VehicleJpaRepository;
import com.apexmotors.repository.InMemoryVehicleRepository;
import com.apexmotors.repository.JsonVehicleGalleryRepository;
import com.apexmotors.repository.VehicleGalleryRepository;
import com.apexmotors.repository.VehicleRepository;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DefaultProfileIsolationTest {
    @Autowired ApplicationContext context;

    @Test
    void showroomRunsWithoutAnyDatabaseInfrastructureEvenWithH2OnTheClasspath() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
        assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
        assertThat(context.getBeansOfType(VehicleJpaRepository.class)).isEmpty();
        assertThat(context.getBean(VehicleRepository.class)).isInstanceOf(InMemoryVehicleRepository.class);
        assertThat(context.getBean(VehicleGalleryRepository.class)).isInstanceOf(JsonVehicleGalleryRepository.class);
    }
}
