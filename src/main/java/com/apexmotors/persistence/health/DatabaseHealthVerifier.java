package com.apexmotors.persistence.health;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Internal startup/test check, deliberately not a controller or public endpoint. */
@Component
@Profile("mysql")
public class DatabaseHealthVerifier {
    private static final Set<String> TABLES = Set.of(
        "roles", "users", "brands", "vehicles", "vehicle_images", "appointments", "favorites");
    private final DataSource dataSource;
    private final Flyway flyway;

    public DatabaseHealthVerifier(DataSource dataSource, Flyway flyway) {
        this.dataSource = dataSource;
        this.flyway = flyway;
    }

    public void verify() throws SQLException {
        flyway.validate();
        var info = flyway.info();
        if (info.current() == null || info.applied().length < 2 || info.pending().length != 0) {
            throw new IllegalStateException("Database migrations are incomplete");
        }
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery("SELECT 1")) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("Database connectivity verification failed");
                }
            }
            Set<String> found = new HashSet<>();
            try (var tables = connection.getMetaData().getTables(
                    connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
                while (tables.next()) found.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
            if (!found.containsAll(TABLES) || !found.contains("flyway_schema_history")) {
                throw new IllegalStateException("Database foundation tables are incomplete");
            }
        }
    }
}
