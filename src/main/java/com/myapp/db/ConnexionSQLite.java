package com.myapp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnexionSQLite {

    private static final Logger logger = LoggerFactory.getLogger(ConnexionSQLite.class);
    private static final String DB_NAME = "gestion.db";

    public static Connection getConnection() throws SQLException {
        File appDir = new File(System.getProperty("user.dir"));
        File dbFile = new File(appDir, DB_NAME);

        if (!dbFile.exists()) {
            throw new SQLException("Base SQLite introuvable : " + dbFile.getAbsolutePath());
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            logger.warn("Impossible d'activer les cles etrangeres : {}", e.getMessage());
        }

        logger.debug("DB connectee : {}", dbFile.getAbsolutePath());
        return conn;
    }
}
