package com.myapp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class InitDB {

    private static final Logger logger = LoggerFactory.getLogger(InitDB.class);

    public static void createTablesIfNeeded() {
        String sqlArticles = "CREATE TABLE IF NOT EXISTS Articles ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "designation TEXT NOT NULL, "
                + "prix REAL NOT NULL, "
                + "stock INTEGER NOT NULL)";

        String sqlFactures = "CREATE TABLE IF NOT EXISTS Factures ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "type TEXT NOT NULL, "
                + "date TEXT NOT NULL, "
                + "client_id INTEGER, "
                + "FOREIGN KEY(client_id) REFERENCES Clients(id))";

        String sqlDetails = "CREATE TABLE IF NOT EXISTS DetailsFacture ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "facture_id INTEGER, "
                + "article_id INTEGER, "
                + "quantite INTEGER, "
                + "prix_unitaire REAL, "
                + "FOREIGN KEY(facture_id) REFERENCES Factures(id), "
                + "FOREIGN KEY(article_id) REFERENCES Articles(id))";

        String sqlClients = "CREATE TABLE IF NOT EXISTS Clients ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nom TEXT NOT NULL)";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement()) {

            st.execute(sqlArticles);
            st.execute(sqlClients);
            st.execute(sqlFactures);
            st.execute(sqlDetails);

            logger.info("Toutes les tables créées (si nécessaire).");
        } catch (Exception e) {
            logger.error("Erreur lors de la création des tables: {}", e.getMessage());
        }
    }

    public static void insertSampleArticlesIfEmpty() {
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement st = conn.createStatement()) {

            ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM Articles");
            if (rs.next() && rs.getInt("c") == 0) {
                st.executeUpdate("INSERT INTO Articles (designation,prix,stock) VALUES "
                        + "('Huile moteur', 15.5, 10), "
                        + "('Filtre à huile', 8.0, 20), "
                        + "('Balai essuie-glace', 12.0, 15)");

                logger.info("Articles exemples insérés.");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'insertion des articles exemples: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        createTablesIfNeeded();
        insertSampleArticlesIfEmpty();
    }
}
