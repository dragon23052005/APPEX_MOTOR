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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class DefaultProfileIsolationTest {
    @Autowired ApplicationContext context;
    @Autowired MockMvc mvc;

    @Test
    void showroomRunsWithoutAnyDatabaseInfrastructureEvenWithH2OnTheClasspath() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
        assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
        assertThat(context.getBeansOfType(VehicleJpaRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
        assertThat(context.getBean(VehicleRepository.class)).isInstanceOf(InMemoryVehicleRepository.class);
        assertThat(context.getBean(VehicleGalleryRepository.class)).isInstanceOf(JsonVehicleGalleryRepository.class);
    }

    @Test
    void accountFeaturesAreUnavailableWhileTheShowroomRemainsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isServiceUnavailable()).andExpect(view().name("auth/unavailable"));
        mvc.perform(get("/profile")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/login\""))));
    }
}
