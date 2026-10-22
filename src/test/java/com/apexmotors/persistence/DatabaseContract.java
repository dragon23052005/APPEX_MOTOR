package com.apexmotors.persistence;

import com.apexmotors.persistence.entity.*;
import com.apexmotors.persistence.health.DatabaseHealthVerifier;
import com.apexmotors.persistence.repository.*;
import com.apexmotors.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs unchanged against H2 and the opt-in real MySQL 8.4 integration instance. */
@Transactional
abstract class DatabaseContract {
    @Autowired VehicleJpaRepository vehicles;
    @Autowired BrandJpaRepository brands;
    @Autowired RoleJpaRepository roles;
    @Autowired UserJpaRepository users;
    @Autowired VehicleImageJpaRepository images;
    @Autowired AppointmentJpaRepository appointments;
    @Autowired FavoriteJpaRepository favorites;
    @Autowired VehicleRepository catalog;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired DatabaseHealthVerifier health;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;

    @Test
    void migrationsAreCurrentAndRerunningDoesNotOverwriteData() throws Exception {
        health.verify();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");
        assertThat(flyway.info().applied()).hasSize(2);
        jdbc.update("UPDATE vehicles SET description = 'Locally edited description' WHERE slug = 'rimac-nevera'");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT description FROM vehicles WHERE slug = 'rimac-nevera'", String.class))
            .isEqualTo("Locally edited description");
        assertThat(roles.findAll()).extracting(RoleEntity::getName)
            .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.CUSTOMER);
        assertThat(users.count()).isZero();
        assertThat(appointments.count()).isZero();
        assertThat(favorites.count()).isZero();
        assertThat(vehicles.count()).isEqualTo(8);
        assertThat(brands.count()).isEqualTo(8);
    }

    @Test
    void everySeedMatchesTheExistingCatalogAndHeroUrl() {
        assertThat(vehicles.findAllByOrderByIdAsc()).hasSize(catalog.findAll().size());
        for (var source : catalog.findAll()) {
            var stored = vehicles.findBySlug(source.getSlug()).orElseThrow();
            assertThat(stored.getName()).isEqualTo(source.getDisplayName());
            assertThat(stored.getBrand().getName()).isEqualTo(source.getManufacturer());
            assertThat(stored.getDescription()).isEqualTo(source.getDescription());
            assertThat(stored.getModelYear()).isEqualTo(source.getYear());
            assertThat(stored.getPrice()).isEqualByComparingTo(source.getPrice());
            assertThat(stored.getCurrency()).isEqualTo("EUR");
            assertThat(stored.getHorsepower()).isEqualTo(source.getHorsepower());
            assertThat(stored.getTopSpeedKmh()).isEqualTo(source.getTopSpeed());
            assertThat(stored.getAcceleration0100()).isEqualByComparingTo(BigDecimal.valueOf(source.getZeroToHundred()));
            assertThat(stored.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.UNSPECIFIED);
            assertThat(images.findByVehicleIdOrderByDisplayOrderAsc(stored.getId())).singleElement().satisfies(image -> {
                assertThat(image.getImageUrl()).isEqualTo(source.getImagePath());
                assertThat(image.getAltText()).isEqualTo(source.getDisplayName());
                assertThat(image.isPrimary()).isTrue();
                assertThat(image.getCategory()).isEqualTo(ImageCategory.HERO);
            });
        }
    }

    @Test
    void persistsRelationshipsWithLazyLoadingAndUtcAuditFields() throws Exception {
        var user = createUser();
        var vehicle = vehicles.findBySlug("ferrari-sf90-stradale").orElseThrow();
        var appointment = new AppointmentEntity();
        appointment.setUser(user);
        appointment.setVehicle(vehicle);
        appointment.setAppointmentAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0));
        appointment.setCustomerNote("Synthetic repository test");
        appointments.saveAndFlush(appointment);
        favorites.saveAndFlush(new FavoriteEntity(user, vehicle));
        Long userId = user.getId();
        Long appointmentId = appointment.getId();
        var favoriteId = new FavoriteId(userId, vehicle.getId());
        var before = user.getUpdatedAt();
        user.setPhone("+84 123 456 789");
        users.flush();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
        entityManager.clear();

        var loaded = appointments.findById(appointmentId).orElseThrow();
        assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(loaded, "user")).isFalse();
        assertThat(loaded.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(mapper.writeValueAsString(loaded)).doesNotContain("password", "\"user\"", "\"vehicle\"");
        var persistedUser = users.findByEmail("repository-test@example.invalid").orElseThrow();
        assertThat(persistedUser.getRole().getName()).isEqualTo(RoleName.CUSTOMER);
        assertThat(persistedUser.isEnabled()).isTrue();
        assertThat(mapper.writeValueAsString(persistedUser)).doesNotContain("passwordHash", "$2b$");
        assertThat(appointments.findByUserIdOrderByAppointmentAtDesc(userId)).hasSize(1);
        assertThat(favorites.findById(favoriteId)).isPresent();
        assertThat(favorites.findByUserIdOrderByCreatedAtDesc(userId)).hasSize(1);
    }

    @Test
    void databaseEnforcesUniqueKeysAndForeignKeys() {
        var user = createUser();
        var vehicle = vehicles.findBySlug("rimac-nevera").orElseThrow();
        favorites.saveAndFlush(new FavoriteEntity(user, vehicle));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO favorites (user_id, vehicle_id) VALUES (?, ?)", user.getId(), vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO roles (name) VALUES ('ADMIN')"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO brands (name, slug) VALUES ('Duplicate', 'ferrari')"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO vehicles (brand_id, name, slug) VALUES (?, 'Duplicate', 'rimac-nevera')", vehicle.getBrand().getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users (role_id, full_name, email, password_hash) SELECT role_id, full_name, email, password_hash FROM users WHERE id = ?", user.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO favorites (user_id, vehicle_id) VALUES (?, ?)", user.getId(), -1L))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidAmountsStatusesAndImageOrdering() {
        var vehicle = vehicles.findBySlug("rimac-nevera").orElseThrow();
        assertThatThrownBy(() -> jdbc.update("UPDATE vehicles SET price = -1 WHERE id = ?", vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE vehicles SET currency = NULL WHERE id = ?", vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE vehicles SET availability_status = 'INVALID' WHERE id = ?", vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO vehicle_images (vehicle_id, image_url, alt_text, category, display_order) VALUES (?, '/test.webp', 'Test', 'HERO', 0)", vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletionCascadesOnlyToDependentMetadataAndFavorites() {
        var user = createUser();
        var vehicle = vehicles.findBySlug("rimac-nevera").orElseThrow();
        favorites.saveAndFlush(new FavoriteEntity(user, vehicle));
        jdbc.update("DELETE FROM vehicles WHERE id = ?", vehicle.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_images WHERE vehicle_id = ?", Long.class, vehicle.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_id = ?", Long.class, user.getId())).isZero();
        assertThat(users.existsById(user.getId())).isTrue();
    }

    @Test
    void appointmentHistoryPreventsDeletingItsUserOrVehicle() {
        var user = createUser();
        var vehicle = vehicles.findBySlug("rimac-nevera").orElseThrow();
        jdbc.update("INSERT INTO appointments (user_id, vehicle_id, appointment_at) VALUES (?, ?, ?)",
            user.getId(), vehicle.getId(), LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM vehicles WHERE id = ?", vehicle.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void showroomAndFerrariRemainAvailableWithPersistenceEnabled() throws Exception {
        mvc.perform(get("/collection")).andExpect(status().isOk());
        mvc.perform(get("/cars/ferrari-sf90-stradale"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("gallery"))
            .andExpect(content().string(containsString("Explore 24 perspectives")));
        mvc.perform(get("/database-health")).andExpect(status().isNotFound());
    }

    private UserEntity createUser() {
        var user = new UserEntity();
        user.setRole(roles.findByName(RoleName.CUSTOMER).orElseThrow());
        user.setFullName("Synthetic repository test");
        user.setEmail(" Repository-Test@Example.Invalid ");
        // Only a structurally valid synthetic hash in a rolled-back test, never a seeded account.
        user.setPasswordHash("$2b$12$" + "a".repeat(53));
        return users.saveAndFlush(user);
    }
}
