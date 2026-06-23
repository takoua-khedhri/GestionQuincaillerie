package com.myapp.service;

import com.myapp.db.ConnexionSQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    public static boolean verifierStock(String designation, int quantiteDemandee) {
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT stock FROM Articles WHERE designation = ?")) {

            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int stockActuel = rs.getInt("stock");
                    return stockActuel >= quantiteDemandee;
                }
            }
        } catch (Exception e) {
            logger.error("Erreur verification stock: {}", e.getMessage());
        }
        return false;
    }

    public static int getStockActuel(String designation) {
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT stock FROM Articles WHERE designation = ?")) {

            pst.setString(1, designation);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        } catch (Exception e) {
            logger.error("Erreur recuperation stock: {}", e.getMessage());
        }
        return 0;
    }

    public static boolean mettreAJourStock(String designation, int quantiteVendue) {
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement("UPDATE Articles SET stock = stock - ? WHERE designation = ?")) {

            pst.setInt(1, quantiteVendue);
            pst.setString(2, designation);

            int rowsAffected = pst.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            logger.error("Erreur mise a jour stock: {}", e.getMessage());
            return false;
        }
    }

    public static boolean verifierStockFacture(DefaultTableModel model) {
        for (int i = 0; i < model.getRowCount(); i++) {
            String designation = model.getValueAt(i, 1).toString();
            int quantite = Integer.parseInt(model.getValueAt(i, 2).toString());

            if (!verifierStock(designation, quantite)) {
                return false;
            }
        }
        return true;
    }

    public static boolean mettreAJourStockFacture(DefaultTableModel model) {
        boolean succesTotal = true;

        for (int i = 0; i < model.getRowCount(); i++) {
            String designation = model.getValueAt(i, 1).toString();
            int quantite = Integer.parseInt(model.getValueAt(i, 2).toString());

            if (!mettreAJourStock(designation, quantite)) {
                succesTotal = false;
            }
        }
        return succesTotal;
    }
}
