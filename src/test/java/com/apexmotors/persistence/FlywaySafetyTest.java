package com.apexmotors.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FlywaySafetyTest {
    @Test
    void refusesAnUnknownNonemptyDatabaseAndLeavesItsDataUntouched() throws Exception {
        String url = "jdbc:h2:mem:unknown_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE existing_customer_data (id INT PRIMARY KEY)");
            statement.execute("INSERT INTO existing_customer_data VALUES (42)");
            var flyway = Flyway.configure().dataSource(url, "sa", "")
                .locations("classpath:db/migration").baselineOnMigrate(false).cleanDisabled(true).load();
            assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class).hasMessageContaining("non-empty");
            assertThatThrownBy(flyway::clean).isInstanceOf(FlywayException.class).hasMessageContaining("cleanDisabled");
            try (var result = statement.executeQuery("SELECT id FROM existing_customer_data")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(42);
            }
        }
    }
}
