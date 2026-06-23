package com.myapp.service;

import com.myapp.db.ConnexionSQLite;
import com.myapp.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public static void logAction(String action, String entite, Integer entiteId, String details) {
        String utilisateur = SessionManager.getInstance().getCurrentUser();
        String sql = "INSERT INTO AuditLog (utilisateur, action, entite, entite_id, details) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, utilisateur);
            pst.setString(2, action);
            pst.setString(3, entite);
            if (entiteId != null) pst.setInt(4, entiteId);
            else pst.setNull(4, java.sql.Types.INTEGER);
            pst.setString(5, details);
            pst.executeUpdate();
            log.debug("Audit: {} - {} {} ({})", utilisateur, action, entite, details);
        } catch (SQLException e) {
            log.error("Erreur enregistrement audit: {}", e.getMessage());
        }
    }

    public static void logAction(String action, String entite, String details) {
        logAction(action, entite, null, details);
    }

    public static List<Object[]> getHistorique(int limit) {
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT id, utilisateur, action, entite, entite_id, details, date_action " +
                "FROM AuditLog ORDER BY date_action DESC LIMIT ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    logs.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("utilisateur"),
                            rs.getString("action"),
                            rs.getString("entite"),
                            rs.getObject("entite_id"),
                            rs.getString("details"),
                            rs.getString("date_action")
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Erreur lecture audit: {}", e.getMessage());
        }
        return logs;
    }

    public static List<Object[]> getHistoriqueParUtilisateur(String utilisateur, int limit) {
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT id, utilisateur, action, entite, entite_id, details, date_action " +
                "FROM AuditLog WHERE utilisateur = ? ORDER BY date_action DESC LIMIT ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, utilisateur);
            pst.setInt(2, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    logs.add(new Object[]{
                            rs.getInt("id"),
                            rs.getString("utilisateur"),
                            rs.getString("action"),
                            rs.getString("entite"),
                            rs.getObject("entite_id"),
                            rs.getString("details"),
                            rs.getString("date_action")
                    });
                }
            }
        } catch (SQLException e) {
            log.error("Erreur lecture audit par utilisateur: {}", e.getMessage());
        }
        return logs;
    }
}
