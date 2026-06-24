package com.myapp.ui.components;

import com.myapp.db.ConnexionSQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Verifie les niveaux de stock et affiche des notifications d'alerte
 * lorsque le stock d'un article est inferieur ou egal a son seuil minimum.
 */
public class StockAlertChecker {

    private static final Logger log = LoggerFactory.getLogger(StockAlertChecker.class);
    private static final int MAX_NOTIFICATIONS = 5;
    private static final String QUERY =
            "SELECT nom, stock, stock_min FROM articles WHERE stock <= stock_min AND stock_min > 0 LIMIT ?";

    private Timer periodicTimer;

    /**
     * Verifie les niveaux de stock et affiche une notification d'avertissement
     * pour chaque article en rupture (maximum 5).
     */
    public void checkAndNotify(Component parent) {
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY)) {

            ps.setInt(1, MAX_NOTIFICATIONS);

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String nom = rs.getString("nom");
                    int stock = rs.getInt("stock");
                    int stockMin = rs.getInt("stock_min");
                    String msg = String.format("Stock bas : %s (%d/%d)", nom, stock, stockMin);
                    NotificationManager.showWarning(parent, msg);
                    count++;
                }
                if (count > 0) {
                    log.info("{} alerte(s) de stock affichee(s)", count);
                } else {
                    log.debug("Aucune alerte de stock");
                }
            }
        } catch (SQLException e) {
            log.error("Erreur lors de la verification des stocks", e);
            NotificationManager.showError(parent, "Erreur de verification des stocks");
        }
    }

    /**
     * Demarre une verification periodique des stocks en arriere-plan.
     *
     * @param parent           le composant parent pour les notifications
     * @param intervalMinutes  l'intervalle entre chaque verification, en minutes
     */
    public void startPeriodicCheck(Component parent, int intervalMinutes) {
        if (periodicTimer != null && periodicTimer.isRunning()) {
            periodicTimer.stop();
            log.info("Verification periodique precedente arretee");
        }

        int intervalMs = intervalMinutes * 60 * 1000;
        periodicTimer = new Timer(intervalMs, e -> checkAndNotify(parent));
        periodicTimer.setInitialDelay(0);
        periodicTimer.start();
        log.info("Verification periodique des stocks demarree (intervalle : {} min)", intervalMinutes);
    }

    /**
     * Arrete la verification periodique des stocks.
     */
    public void stopPeriodicCheck() {
        if (periodicTimer != null && periodicTimer.isRunning()) {
            periodicTimer.stop();
            log.info("Verification periodique des stocks arretee");
        }
    }
}
