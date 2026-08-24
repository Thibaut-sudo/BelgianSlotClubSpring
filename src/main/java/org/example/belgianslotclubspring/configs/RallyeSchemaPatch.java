package org.example.belgianslotclubspring.configs;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hibernate {@code ddl-auto=update} n’ajoute pas toujours les nouvelles colonnes
 * sur une base H2 fichier déjà existante. On complète au démarrage.
 */
@Component
@DependsOn("entityManagerFactory")
public class RallyeSchemaPatch {

    private static final Logger log = LoggerFactory.getLogger(RallyeSchemaPatch.class);

    private final DataSource dataSource;

    public RallyeSchemaPatch(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void addFinishedColumn() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (hasFinishedColumn(statement)) {
                log.info("Colonne rallye.finished déjà présente.");
                return;
            }
            statement.execute(
                    "ALTER TABLE rallye ADD COLUMN IF NOT EXISTS finished BOOLEAN DEFAULT FALSE NOT NULL"
            );
            log.info("Colonne rallye.finished ajoutée.");
        } catch (SQLException e) {
            log.warn("Impossible d’ajouter rallye.finished : {}", e.getMessage());
        }
    }

    private static boolean hasFinishedColumn(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = 'RALLYE' AND UPPER(COLUMN_NAME) = 'FINISHED'
                """)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
