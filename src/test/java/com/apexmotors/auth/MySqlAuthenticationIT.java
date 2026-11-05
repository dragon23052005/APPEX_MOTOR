package com.apexmotors.auth;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;

/** Runs the account contract on an isolated MySQL server; 8.4 remains the default target. */
@Testcontainers
@ActiveProfiles(value = "mysql", inheritProfiles = false)
class MySqlAuthenticationIT extends AuthenticationFlowTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
        System.getProperty("apex.mysql.test-image", "mysql:8.4"))
        .withDatabaseName("apex_auth_test")
        .withUsername("apex_auth_test")
        .withPassword(UUID.randomUUID().toString())
        .withUrlParam("connectionTimeZone", "UTC")
        .withUrlParam("forceConnectionTimeZoneToSession", "true")
        .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci",
            "--default-time-zone=+00:00")
        .withCreateContainerCmdModifier(command -> command.getHostConfig()
            .withPortBindings(new com.github.dockerjava.api.model.PortBinding(
                com.github.dockerjava.api.model.Ports.Binding.bindIpAndPort("127.0.0.1", 0),
                new com.github.dockerjava.api.model.ExposedPort(3306))));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        // PER_CLASS fixtures can initialize the Spring context before JUnit's beforeAll hook.
        MYSQL.start();
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }
}
