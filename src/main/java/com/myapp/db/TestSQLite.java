package com.myapp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class TestSQLite {

    private static final Logger log = LoggerFactory.getLogger(TestSQLite.class);

    public static void main(String[] args) {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            log.info("Connexion reussie !");
        } catch (Exception e) {
            log.error("Erreur de connexion : {}", e.getMessage());
        }
    }
}
