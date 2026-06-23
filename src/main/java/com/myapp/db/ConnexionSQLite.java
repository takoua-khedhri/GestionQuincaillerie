package com.myapp.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement; // Ajout de l'import Statement

public class ConnexionSQLite {
    private static final String DB_NAME = "gestion.db";

    public static Connection getConnection() throws SQLException {
        File appDir = new File(System.getProperty("user.dir"));
        File dbFile = new File(appDir, "gestion.db");

        if (!dbFile.exists()) {
            throw new SQLException("❌ Base SQLite introuvable : " + dbFile.getAbsolutePath());
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        // 1. Établir la connexion
        Connection conn = DriverManager.getConnection(url);
        
        // 2. ACTIVER LE SUPPORT DES CLÉS ÉTRANGÈRES (PRAGMA foreign_keys = ON)
        // Cela permet au "ON DELETE CASCADE" de fonctionner
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            System.err.println("⚠️ Impossible d'activer les clés étrangères : " + e.getMessage());
        }

        System.out.println("✅ DB connectée avec cascade support : " + dbFile.getAbsolutePath());
        return conn;
    }
}