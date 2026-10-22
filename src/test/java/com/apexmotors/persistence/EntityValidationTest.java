package com.apexmotors.persistence;

import com.apexmotors.persistence.entity.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EntityValidationTest {
    @Test
    void rejectsPlaintextPasswordsInvalidEmailAndMissingRole() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var user = new UserEntity();
            user.setFullName("Synthetic test");
            user.setEmail("invalid");
            user.setPasswordHash("plaintext-is-not-an-encoded-hash");
            assertThat(factory.getValidator().validate(user)).extracting(v -> v.getPropertyPath().toString())
                .contains("passwordHash", "email", "role");
        }
    }

    @Test
    void validatesPrecisionSlugsAndPriceCurrencyPair() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var vehicle = new VehicleEntity();
            vehicle.setName("Synthetic test");
            vehicle.setBrand(new BrandEntity());
            vehicle.setSlug("Unsafe slug!");
            vehicle.setPrice(new BigDecimal("-1.001"));
            vehicle.setAcceleration0100(BigDecimal.ZERO);
            vehicle.setModelYear(100);
            assertThat(factory.getValidator().validate(vehicle)).extracting(v -> v.getPropertyPath().toString())
                .contains("slug", "price", "priceAndCurrencyConsistent", "acceleration0100", "modelYear");
        }
    }

    @Test
    void favoriteIdentityUsesBothKeys() {
        assertThat(new FavoriteId(1L, 2L)).isEqualTo(new FavoriteId(1L, 2L));
        assertThat(new FavoriteId(1L, 2L).hashCode()).isEqualTo(new FavoriteId(1L, 2L).hashCode());
        assertThat(new FavoriteId(1L, 2L)).isNotEqualTo(new FavoriteId(2L, 1L));
    }
}
