package com.myapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private boolean isLoggedIn = false;
    private String currentUser = null;
    private String userRole = null;
    private String role = null;
    private LocalDateTime loginTime = null;

    private static volatile SessionManager instance;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void startSession(String username, String role) {
        this.currentUser = username;
        this.userRole = role;
        this.role = role;
        this.loginTime = LocalDateTime.now();
        this.isLoggedIn = true;

        logger.info("Session ouverte pour: {} a {}",
                username, this.loginTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    public void endSession() {
        if (this.isLoggedIn && this.loginTime != null && this.currentUser != null) {
            Duration sessionDuration = Duration.between(this.loginTime, LocalDateTime.now());
            long hours = sessionDuration.toHours();
            long minutes = sessionDuration.toMinutes() % 60L;

            logger.info("Session fermee pour: {} (Duree: {}h {}m)", this.currentUser, hours, minutes);
        } else {
            logger.info("Aucune session active a fermer");
        }

        this.currentUser = null;
        this.userRole = null;
        this.role = null;
        this.loginTime = null;
        this.isLoggedIn = false;
    }

    public boolean isSessionActive() {
        return this.isLoggedIn && this.loginTime != null && !isSessionExpired();
    }

    public String getCurrentUser() {
        return (this.currentUser != null) ? this.currentUser : "Non connecte";
    }

    public String getUserRole() {
        return (this.userRole != null) ? this.userRole : "Aucun role";
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        if (!"administrateur".equals(role) && !"magasinier".equals(role)) {
            throw new IllegalArgumentException("Role invalide: " + role + ". Valeurs acceptees: administrateur, magasinier");
        }
        this.role = role;
    }

    public boolean isAdministrateur() {
        return "administrateur".equals(this.role);
    }

    public boolean isMagasinier() {
        return "magasinier".equals(this.role);
    }

    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    public String getSessionDuration() {
        if (this.loginTime == null) {
            return "0 minute";
        }

        try {
            Duration duration = Duration.between(this.loginTime, LocalDateTime.now());
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60L;

            if (hours > 0L) {
                return hours + "h " + minutes + "m";
            }
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } catch (Exception e) {
            return "Erreur calcul duree";
        }
    }

    public boolean isSessionExpired() {
        if (this.loginTime == null) return true;

        try {
            LocalDateTime expiryTime = this.loginTime.plusHours(8L);
            return LocalDateTime.now().isAfter(expiryTime);
        } catch (Exception e) {
            return true;
        }
    }

    public String getRemainingTime() {
        if (this.loginTime == null || !this.isLoggedIn) return "Session inactive";

        try {
            LocalDateTime expiryTime = this.loginTime.plusHours(8L);
            Duration remaining = Duration.between(LocalDateTime.now(), expiryTime);

            if (remaining.isNegative()) {
                return "Session expiree";
            }

            long hours = remaining.toHours();
            long minutes = remaining.toMinutes() % 60L;

            return hours + "h " + minutes + "m";
        } catch (Exception e) {
            return "Erreur calcul";
        }
    }
}
