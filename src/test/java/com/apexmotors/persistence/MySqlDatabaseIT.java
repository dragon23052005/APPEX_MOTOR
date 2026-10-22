package com.apexmotors.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Requires Docker; fails rather than silently passing/skipping when explicitly requested. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.address=127.0.0.1")
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
@Testcontainers
class MySqlDatabaseIT extends DatabaseContract {
    @Autowired TestRestTemplate http;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("apex_motors")
        .withUsername("apex_test")
        .withPassword(UUID.randomUUID().toString())
        .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci", "--default-time-zone=+00:00")
        .withCreateContainerCmdModifier(command -> command.getHostConfig()
            .withPortBindings(new com.github.dockerjava.api.model.PortBinding(
                com.github.dockerjava.api.model.Ports.Binding.bindIpAndPort("127.0.0.1", 0),
                new com.github.dockerjava.api.model.ExposedPort(3306))));

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "&connectionTimeZone=UTC");
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void serverIsMySql84WithUtf8mb4AndInnoDb() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.4.");
        assertThat(jdbc.queryForObject("SELECT @@character_set_database", String.class)).isEqualTo("utf8mb4");
        List<String> engines = jdbc.queryForList("SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE()", String.class);
        assertThat(engines).isNotEmpty().allMatch("InnoDB"::equals);
    }

    @Test
    void mysqlProfileStartsARealHttpServerAndServesTheFerrariGallery() {
        var response = http.getForEntity("/cars/ferrari-sf90-stradale", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Explore 24 perspectives", "vehicle-gallery.js");
    }
}
