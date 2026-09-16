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
    public void patch() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            addColumn(statement, "MARKETPLACE_LISTING", "SELLER_PASSWORD_HASH",
                    "ALTER TABLE marketplace_listing ADD COLUMN IF NOT EXISTS seller_password_hash VARCHAR(128)");
            addColumn(statement, "MARKETPLACE_LISTING", "OWNER_ACCOUNT_ID",
                    "ALTER TABLE marketplace_listing ADD COLUMN IF NOT EXISTS owner_account_id BIGINT");
            addColumn(statement, "MARKETPLACE_THREAD", "BUYER_ACCOUNT_ID",
                    "ALTER TABLE marketplace_thread ADD COLUMN IF NOT EXISTS buyer_account_id BIGINT");
            makeNullable(statement, "MARKETPLACE_THREAD", "BUYER_PASSWORD_HASH");
        } catch (SQLException e) {
            log.warn("Impossible d’appliquer le patch marketplace : {}", e.getMessage());
        }
    }

    private static void addColumn(Statement statement, String table, String column, String sql) {
        try {
            if (hasColumn(statement, table, column)) {
                log.info("Colonne {}.{} déjà présente.", table.toLowerCase(), column.toLowerCase());
                return;
            }
            statement.execute(sql);
            log.info("Colonne {}.{} ajoutée.", table.toLowerCase(), column.toLowerCase());
        } catch (SQLException e) {
            log.warn("Impossible d’ajouter {}.{} : {}", table.toLowerCase(), column.toLowerCase(), e.getMessage());
        }
    }

    private static void makeNullable(Statement statement, String table, String column) {
        if (!isNotNull(statement, table, column)) {
            return;
        }
        try {
            statement.execute("ALTER TABLE marketplace_thread ALTER COLUMN buyer_password_hash DROP NOT NULL");
            log.info("Colonne marketplace_thread.buyer_password_hash rendue nullable.");
            return;
        } catch (SQLException ignored) {
            // H2 utilise une autre syntaxe.
        }
        try {
            statement.execute("ALTER TABLE marketplace_thread ALTER COLUMN buyer_password_hash SET NULL");
            log.info("Colonne marketplace_thread.buyer_password_hash rendue nullable.");
        } catch (SQLException e) {
            log.warn("Impossible de rendre marketplace_thread.buyer_password_hash nullable : {}", e.getMessage());
        }
    }

    private static boolean hasColumn(Statement statement, String table, String column) throws SQLException {
        try (ResultSet rs = statement.executeQuery("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = '%s'
                  AND UPPER(COLUMN_NAME) = '%s'
                """.formatted(table, column))) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static boolean isNotNull(Statement statement, String table, String column) {
        try (ResultSet rs = statement.executeQuery("""
                SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = '%s'
                  AND UPPER(COLUMN_NAME) = '%s'
                """.formatted(table, column))) {
            return rs.next() && "NO".equalsIgnoreCase(rs.getString(1));
        } catch (SQLException e) {
            return false;
        }
    }
}
