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
 * sur une base H2 fichier déjà existante.
 */
@Component
@DependsOn("entityManagerFactory")
public class MarketplaceSchemaPatch {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceSchemaPatch.class);

    private final DataSource dataSource;

    public MarketplaceSchemaPatch(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void addSellerPasswordHash() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            if (hasColumn(statement)) {
                log.info("Colonne marketplace_listing.seller_password_hash déjà présente.");
                return;
            }
            statement.execute(
                    "ALTER TABLE marketplace_listing ADD COLUMN IF NOT EXISTS seller_password_hash VARCHAR(128)"
            );
            log.info("Colonne marketplace_listing.seller_password_hash ajoutée.");
        } catch (SQLException e) {
            log.warn("Impossible d’ajouter marketplace_listing.seller_password_hash : {}", e.getMessage());
        }
    }

    private static boolean hasColumn(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = 'MARKETPLACE_LISTING'
                  AND UPPER(COLUMN_NAME) = 'SELLER_PASSWORD_HASH'
                """)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
