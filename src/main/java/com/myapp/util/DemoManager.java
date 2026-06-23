package com.myapp.util;

import com.myapp.db.ConnexionSQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DemoManager {

    private static final Logger logger = LoggerFactory.getLogger(DemoManager.class);

    private static final int DEMO_DURATION_DAYS = 7;
    private static final long MINUTES_PER_DAY = 1440L;
    private static final long DEMO_DURATION_MINUTES = 10080L;

    public static LocalDateTime getDemoStartTime() {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            String sql = "SELECT start_time FROM demo WHERE id = 1";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return LocalDateTime.parse(rs.getString("start_time"));
                }
            }

            LocalDateTime now = LocalDateTime.now();
            String insertSql = "INSERT INTO demo (id, start_time, expired) VALUES (1, ?, 0)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                psInsert.setString(1, now.toString());
                psInsert.executeUpdate();
            }
            return now;

        } catch (Exception e) {
            logger.error("Erreur lors de la recuperation du temps de demo: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    public static boolean isDemoExpired() {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            String checkExpiredSql = "SELECT expired FROM demo WHERE id = 1";
            try (PreparedStatement psCheck = conn.prepareStatement(checkExpiredSql);
                 ResultSet rsCheck = psCheck.executeQuery()) {

                if (rsCheck.next() && rsCheck.getInt("expired") == 1) {
                    return true;
                }
            }

            LocalDateTime start = getDemoStartTime();
            LocalDateTime now = LocalDateTime.now();
            long minutesElapsed = ChronoUnit.MINUTES.between(start, now);

            if (minutesElapsed >= DEMO_DURATION_MINUTES) {
                String updateSql = "UPDATE demo SET expired = 1 WHERE id = 1";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.executeUpdate();
                }
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Erreur lors de la verification d'expiration demo: {}", e.getMessage());
            return false;
        }
    }

    public static long getRemainingMinutes() {
        try {
            LocalDateTime start = getDemoStartTime();
            LocalDateTime now = LocalDateTime.now();
            long elapsedMinutes = ChronoUnit.MINUTES.between(start, now);
            long remainingMinutes = DEMO_DURATION_MINUTES - elapsedMinutes;
            return Math.max(0L, remainingMinutes);
        } catch (Exception e) {
            logger.error("Erreur calcul minutes restantes: {}", e.getMessage());
            return 0L;
        }
    }

    public static long getRemainingDays() {
        try {
            LocalDateTime start = getDemoStartTime();
            LocalDateTime now = LocalDateTime.now();
            long daysElapsed = ChronoUnit.DAYS.between(start, now);
            long remainingDays = DEMO_DURATION_DAYS - daysElapsed;

            if (remainingDays < 0L) remainingDays = 0L;
            if (remainingDays > DEMO_DURATION_DAYS) remainingDays = DEMO_DURATION_DAYS;

            return remainingDays;
        } catch (Exception e) {
            logger.error("Erreur calcul jours restants: {}", e.getMessage());
            return DEMO_DURATION_DAYS;
        }
    }

    public static void resetDemo() {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            String sql = "DELETE FROM demo WHERE id = 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Erreur lors du reset demo: {}", e.getMessage());
        }
    }

    public static long getElapsedDays() {
        try {
            LocalDateTime start = getDemoStartTime();
            LocalDateTime now = LocalDateTime.now();
            long elapsedDays = ChronoUnit.DAYS.between(start, now);
            return Math.min(elapsedDays, DEMO_DURATION_DAYS);
        } catch (Exception e) {
            logger.error("Erreur calcul jours ecoules: {}", e.getMessage());
            return 0L;
        }
    }

    public static int getDemoDurationDays() {
        return DEMO_DURATION_DAYS;
    }
}
