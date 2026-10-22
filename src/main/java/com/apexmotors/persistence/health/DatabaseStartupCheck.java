package com.apexmotors.persistence.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.sql.SQLException;

@Component
@Profile("mysql")
public class DatabaseStartupCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupCheck.class);
    private final DatabaseHealthVerifier verifier;

    public DatabaseStartupCheck(DatabaseHealthVerifier verifier) { this.verifier = verifier; }

    @Override
    public void run(ApplicationArguments arguments) throws SQLException {
        verifier.verify();
        log.info("Database foundation verified: connectivity, migrations and seven application tables");
    }
}
