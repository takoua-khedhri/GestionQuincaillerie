package com.myapp.service;

import com.myapp.db.ConnexionSQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaiementService {

    private static final Logger logger = LoggerFactory.getLogger(PaiementService.class);

    public static boolean enregistrerPaiement(int factureId, String typeDocument, double montant,
                                               String modePaiement, String reference, String notes) {
        String datePaiement = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Inserer le paiement
                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO Paiements (facture_id, type_document, date_paiement, montant, " +
                        "mode_paiement, reference, notes) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    pst.setInt(1, factureId);
                    pst.setString(2, typeDocument);
                    pst.setString(3, datePaiement);
                    pst.setDouble(4, montant);
                    pst.setString(5, modePaiement);
                    pst.setString(6, reference);
                    pst.setString(7, notes);
                    pst.executeUpdate();
                }

                // Calculer le total paye
                double totalPaye = 0;
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT COALESCE(SUM(montant), 0) as total FROM Paiements WHERE facture_id = ?")) {
                    pst.setInt(1, factureId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            totalPaye = rs.getDouble("total");
                        }
                    }
                }

                // Recuperer le montant TTC de la facture
                double montantTtc = 0;
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT montant_ttc FROM Factures WHERE id = ?")) {
                    pst.setInt(1, factureId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            montantTtc = rs.getDouble("montant_ttc");
                        }
                    }
                }

                // Determiner le statut de paiement
                String statutPaiement;
                if (totalPaye >= montantTtc) {
                    statutPaiement = "PAYE";
                } else if (totalPaye > 0) {
                    statutPaiement = "PARTIEL";
                } else {
                    statutPaiement = "NON_PAYE";
                }

                // Mettre a jour la facture
                try (PreparedStatement pst = conn.prepareStatement(
                        "UPDATE Factures SET montant_paye = ?, statut_paiement = ? WHERE id = ?")) {
                    pst.setDouble(1, totalPaye);
                    pst.setString(2, statutPaiement);
                    pst.setInt(3, factureId);
                    pst.executeUpdate();
                }

                conn.commit();
                logger.info("Paiement de {} enregistre pour la facture {} - Statut: {}",
                        montant, factureId, statutPaiement);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Erreur enregistrement paiement: {}", e.getMessage());
            return false;
        }
    }

    public static List<Map<String, Object>> getPaiementsByFacture(int factureId) {
        List<Map<String, Object>> paiements = new ArrayList<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT * FROM Paiements WHERE facture_id = ? ORDER BY date_paiement DESC")) {
            pst.setInt(1, factureId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> paiement = new HashMap<>();
                    paiement.put("id", rs.getInt("id"));
                    paiement.put("facture_id", rs.getInt("facture_id"));
                    paiement.put("type_document", rs.getString("type_document"));
                    paiement.put("date_paiement", rs.getString("date_paiement"));
                    paiement.put("montant", rs.getDouble("montant"));
                    paiement.put("mode_paiement", rs.getString("mode_paiement"));
                    paiement.put("reference", rs.getString("reference"));
                    paiement.put("notes", rs.getString("notes"));
                    paiements.add(paiement);
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur recuperation paiements: {}", e.getMessage());
        }
        return paiements;
    }

    public static double getMontantRestant(int factureId) {
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT COALESCE(f.montant_ttc, 0) - COALESCE(f.montant_paye, 0) as restant " +
                     "FROM Factures f WHERE f.id = ?")) {
            pst.setInt(1, factureId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("restant");
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur calcul montant restant: {}", e.getMessage());
        }
        return 0;
    }

    public static List<Map<String, Object>> getFacturesImpayees() {
        List<Map<String, Object>> factures = new ArrayList<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM Factures WHERE statut_paiement IS NULL " +
                     "OR statut_paiement = 'NON_PAYE' OR statut_paiement = 'PARTIEL' " +
                     "ORDER BY date DESC")) {
            while (rs.next()) {
                Map<String, Object> facture = new HashMap<>();
                facture.put("id", rs.getInt("id"));
                facture.put("numero", rs.getString("numero"));
                facture.put("date", rs.getString("date"));
                facture.put("client_nom", rs.getString("client_nom"));
                facture.put("montant_ttc", rs.getDouble("montant_ttc"));
                facture.put("montant_paye", rs.getDouble("montant_paye"));
                facture.put("statut_paiement", rs.getString("statut_paiement"));
                factures.add(facture);
            }
        } catch (SQLException e) {
            logger.error("Erreur recuperation factures impayees: {}", e.getMessage());
        }
        return factures;
    }
}
