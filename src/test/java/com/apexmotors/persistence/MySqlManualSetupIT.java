package com.apexmotors.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the SQL-client entry point without a preselected or precreated database. */
@Testcontainers
class MySqlManualSetupIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
        System.getProperty("apex.mysql.manual-test-image", "mysql:8.4"))
        .withDatabaseName("manual_setup_test")
        .withUsername("root")
        .withPassword(UUID.randomUUID().toString())
        .withCreateContainerCmdModifier(command -> command.getHostConfig()
            .withPortBindings(new com.github.dockerjava.api.model.PortBinding(
                com.github.dockerjava.api.model.Ports.Binding.bindIpAndPort("127.0.0.1", 0),
                new com.github.dockerjava.api.model.ExposedPort(3306))));

    @Test
    void createsDatabaseAndTablesFromAnUnselectedConnectionAndPreservesDataOnRerun() throws Exception {
        String url = "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306)
            + "/?allowPublicKeyRetrieval=true&sslMode=DISABLED&connectionTimeZone=UTC";
        var foundation = new ClassPathResource("db/migration/V1__database_foundation.sql");

        try (var connection = DriverManager.getConnection(url, MYSQL.getUsername(), MYSQL.getPassword())) {
            assertThat(scalar(connection, "SELECT DATABASE()")).isNull();
            assertThat(scalar(connection, "SELECT COUNT(*) FROM information_schema.SCHEMATA"
                + " WHERE SCHEMA_NAME = 'apex_motors'")).isEqualTo("0");

            ScriptUtils.executeSqlScript(connection, foundation);

            assertThat(scalar(connection, "SELECT DATABASE()")).isEqualTo("apex_motors");
            assertThat(scalar(connection, "SELECT @@session.time_zone")).isEqualTo("+00:00");
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT TABLE_NAME, ENGINE, TABLE_COLLATION"
                     + " FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'apex_motors'")) {
                var names = new ArrayList<String>();
                while (rows.next()) {
                    names.add(rows.getString(1));
                    assertThat(rows.getString(2)).isEqualTo("InnoDB");
                    assertThat(rows.getString(3)).isEqualTo("utf8mb4_0900_ai_ci");
                }
                assertThat(names).containsExactlyInAnyOrder("roles", "users", "brands", "vehicles",
                    "vehicle_images", "appointments", "favorites");
            }

            ScriptUtils.executeSqlScript(connection,
                new ClassPathResource("db/migration/V2__seed_showroom_catalog.sql"));
            assertThat(scalar(connection, "SELECT COUNT(*) FROM roles")).isEqualTo("2");
            assertThat(scalar(connection, "SELECT COUNT(*) FROM vehicles")).isEqualTo("8");
            assertThat(scalar(connection, "SELECT COUNT(*) FROM vehicle_images")).isEqualTo("8");
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE brands SET description = 'Local edit' WHERE slug = 'ferrari'");
                assertThatThrownBy(() -> statement.executeUpdate(
                    "INSERT INTO vehicles (brand_id, name, slug) VALUES (-1, 'Invalid', 'invalid-fk')"))
                    .isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE vehicles SET price = -1 WHERE slug = 'ferrari-sf90-stradale'"))
                    .isInstanceOf(SQLException.class);
            }

            // Running the file twice must fail on existing tables without dropping any data.
            assertThatThrownBy(() -> ScriptUtils.executeSqlScript(connection, foundation))
                .isInstanceOf(ScriptException.class).hasRootCauseInstanceOf(SQLException.class);
            assertThat(scalar(connection, "SELECT description FROM brands WHERE slug = 'ferrari'"))
                .isEqualTo("Local edit");
            assertThat(scalar(connection, "SELECT COUNT(*) FROM vehicles")).isEqualTo("8");
            assertThat(scalar(connection, "SELECT COUNT(*) FROM information_schema.TABLES"
                + " WHERE TABLE_SCHEMA = 'manual_setup_test'")).isEqualTo("0");
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).isTrue();
            return rows.getString(1);
        }
    }
}
