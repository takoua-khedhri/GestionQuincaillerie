package com.myapp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    public static void migrate() {
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement()) {

            addColumnIfNotExists(stmt, "admin", "role", "TEXT NOT NULL DEFAULT 'administrateur'");
            addColumnIfNotExists(stmt, "Articles", "stock_min", "INTEGER DEFAULT 5");

            stmt.execute("CREATE TABLE IF NOT EXISTS AuditLog (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "utilisateur TEXT NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "entite TEXT, " +
                    "entite_id INTEGER, " +
                    "details TEXT, " +
                    "date_action DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS MouvementsStock (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "article_designation TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "quantite INTEGER NOT NULL, " +
                    "reference_document TEXT, " +
                    "type_document TEXT, " +
                    "date_mouvement DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "utilisateur TEXT)");

            log.info("Migration de la base de donnees terminee");
        } catch (SQLException e) {
            log.error("Erreur migration base de donnees", e);
        }
    }

    private static void addColumnIfNotExists(Statement stmt, String table, String column, String definition) {
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")");
            boolean exists = false;
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
            rs.close();
            if (!exists) {
                stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                log.info("Colonne ajoutee : {}.{}", table, column);
            }
        } catch (SQLException e) {
            log.debug("Colonne {}.{} probablement deja existante", table, column);
        }
    }
}
