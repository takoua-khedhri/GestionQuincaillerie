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

public class AvoirService {

    private static final Logger logger = LoggerFactory.getLogger(AvoirService.class);

    public static int creerAvoir(int factureId, String motif, List<Map<String, Object>> articlesRetour) {
        String numero = genererNumeroAvoir();
        String dateAvoir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String clientNom = "";
                try (PreparedStatement pst = conn.prepareStatement(
                        "SELECT client_nom FROM Factures WHERE id = ?")) {
                    pst.setInt(1, factureId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) {
                            clientNom = rs.getString("client_nom");
                        }
                    }
                }

                double montantHt = 0;
                try (PreparedStatement pst = conn.prepareStatement(
                        "INSERT INTO Avoirs (numero, facture_id, date_avoir, client_nom, motif) " +
                        "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    pst.setString(1, numero);
                    pst.setInt(2, factureId);
                    pst.setString(3, dateAvoir);
                    pst.setString(4, clientNom);
                    pst.setString(5, motif);
                    pst.executeUpdate();

                    ResultSet keys = pst.getGeneratedKeys();
                    int avoirId = keys.next() ? keys.getInt(1) : -1;

                    if (avoirId == -1) {
                        conn.rollback();
                        return -1;
                    }

                    for (Map<String, Object> item : articlesRetour) {
                        String designation = (String) item.get("designation");
                        int qteRetour = (Integer) item.get("quantite");
                        double prixUnitaire = ((Number) item.get("prix_unitaire")).doubleValue();
                        double montant = prixUnitaire * qteRetour;
                        montantHt += montant;

                        try (PreparedStatement pstDetail = conn.prepareStatement(
                                "INSERT INTO DetailsAvoir (avoir_id, article_designation, quantite, prix_unitaire, montant) " +
                                "VALUES (?, ?, ?, ?, ?)")) {
                            pstDetail.setInt(1, avoirId);
                            pstDetail.setString(2, designation);
                            pstDetail.setInt(3, qteRetour);
                            pstDetail.setDouble(4, prixUnitaire);
                            pstDetail.setDouble(5, montant);
                            pstDetail.executeUpdate();
                        }

                        // Remettre en stock
                        try (PreparedStatement pstStock = conn.prepareStatement(
                                "UPDATE Articles SET stock = stock + ? WHERE designation = ?")) {
                            pstStock.setInt(1, qteRetour);
                            pstStock.setString(2, designation);
                            pstStock.executeUpdate();
                        }
                    }

                    // Mettre a jour les montants de l'avoir
                    double tva = montantHt * 0.19;
                    double montantTtc = montantHt + tva;
                    try (PreparedStatement pstUpdate = conn.prepareStatement(
                            "UPDATE Avoirs SET montant_ht = ?, tva = ?, montant_ttc = ? WHERE id = ?")) {
                        pstUpdate.setDouble(1, montantHt);
                        pstUpdate.setDouble(2, tva);
                        pstUpdate.setDouble(3, montantTtc);
                        pstUpdate.setInt(4, avoirId);
                        pstUpdate.executeUpdate();
                    }

                    conn.commit();
                    logger.info("Avoir {} cree pour la facture {}", numero, factureId);
                    return avoirId;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Erreur creation avoir: {}", e.getMessage());
            return -1;
        }
    }

    public static List<Map<String, Object>> getAvoirsByFacture(int factureId) {
        List<Map<String, Object>> avoirs = new ArrayList<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT * FROM Avoirs WHERE facture_id = ? ORDER BY created_at DESC")) {
            pst.setInt(1, factureId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> avoir = new HashMap<>();
                    avoir.put("id", rs.getInt("id"));
                    avoir.put("numero", rs.getString("numero"));
                    avoir.put("facture_id", rs.getInt("facture_id"));
                    avoir.put("date_avoir", rs.getString("date_avoir"));
                    avoir.put("client_nom", rs.getString("client_nom"));
                    avoir.put("motif", rs.getString("motif"));
                    avoir.put("montant_ht", rs.getDouble("montant_ht"));
                    avoir.put("tva", rs.getDouble("tva"));
                    avoir.put("montant_ttc", rs.getDouble("montant_ttc"));
                    avoir.put("statut", rs.getString("statut"));
                    avoirs.add(avoir);
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur recuperation avoirs: {}", e.getMessage());
        }
        return avoirs;
    }

    public static List<Map<String, Object>> getAllAvoirs() {
        List<Map<String, Object>> avoirs = new ArrayList<>();
        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Avoirs ORDER BY created_at DESC")) {
            while (rs.next()) {
                Map<String, Object> avoir = new HashMap<>();
                avoir.put("id", rs.getInt("id"));
                avoir.put("numero", rs.getString("numero"));
                avoir.put("facture_id", rs.getInt("facture_id"));
                avoir.put("date_avoir", rs.getString("date_avoir"));
                avoir.put("client_nom", rs.getString("client_nom"));
                avoir.put("motif", rs.getString("motif"));
                avoir.put("montant_ht", rs.getDouble("montant_ht"));
                avoir.put("tva", rs.getDouble("tva"));
                avoir.put("montant_ttc", rs.getDouble("montant_ttc"));
                avoir.put("statut", rs.getString("statut"));
                avoirs.add(avoir);
            }
        } catch (SQLException e) {
            logger.error("Erreur recuperation avoirs: {}", e.getMessage());
        }
        return avoirs;
    }

    public static String genererNumeroAvoir() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "AV-" + dateStr + "-";
        int sequence = 1;

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                     "SELECT numero FROM Avoirs WHERE numero LIKE ? ORDER BY numero DESC LIMIT 1")) {
            pst.setString(1, prefix + "%");
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String lastNumero = rs.getString("numero");
                    String seqStr = lastNumero.substring(lastNumero.lastIndexOf('-') + 1);
                    sequence = Integer.parseInt(seqStr) + 1;
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur generation numero avoir: {}", e.getMessage());
        }

        return prefix + String.format("%03d", sequence);
    }
}
