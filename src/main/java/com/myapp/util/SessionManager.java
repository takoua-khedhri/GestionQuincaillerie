/*     */ package com.myapp.util;
/*     */ 
/*     */ import java.time.Duration;
/*     */ import java.time.LocalDateTime;
/*     */ import java.time.format.DateTimeFormatter;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class SessionManager
/*     */ {
/*     */   private boolean isLoggedIn = false;
/*  17 */   private String currentUser = null;
/*  18 */   private String userRole = null;
/*  19 */   private LocalDateTime loginTime = null;
/*     */   private static SessionManager instance;
/*     */   
/*     */   public static SessionManager getInstance() {
/*  23 */     if (instance == null) {
/*  24 */       instance = new SessionManager();
/*     */     }
/*  26 */     return instance;
/*     */   }
/*     */ 
/*     */   
/*     */   public void startSession(String username, String role) {
/*  31 */     this.currentUser = username;
/*  32 */     this.userRole = role;
/*  33 */     this.loginTime = LocalDateTime.now();
/*  34 */     this.isLoggedIn = true;
/*     */     
/*  36 */     System.out.println("✅ Session ouverte pour: " + username + " à " + this.loginTime
/*  37 */         .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
/*     */   }
/*     */ 
/*     */   
/*     */   public void endSession() {
/*  42 */     if (this.isLoggedIn && this.loginTime != null && this.currentUser != null) {
/*  43 */       Duration sessionDuration = Duration.between(this.loginTime, LocalDateTime.now());
/*  44 */       long minutes = sessionDuration.toMinutes();
/*  45 */       long hours = sessionDuration.toHours();
/*  46 */       minutes %= 60L;
/*     */       
/*  48 */       System.out.println("🔒 Session fermée pour: " + this.currentUser + " (Durée: " + hours + "h " + minutes + "m)");
/*     */     } else {
/*     */       
/*  51 */       System.out.println("🔒 Aucune session active à fermer");
/*     */     } 
/*     */     
/*  54 */     this.currentUser = null;
/*  55 */     this.userRole = null;
/*  56 */     this.loginTime = null;
/*  57 */     this.isLoggedIn = false;
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean isSessionActive() {
/*  62 */     return (this.isLoggedIn && this.loginTime != null && !isSessionExpired());
/*     */   }
/*     */   
/*     */   public String getCurrentUser() {
/*  66 */     return (this.currentUser != null) ? this.currentUser : "Non connecté";
/*     */   }
/*     */   
/*     */   public String getUserRole() {
/*  70 */     return (this.userRole != null) ? this.userRole : "Aucun rôle";
/*     */   }
/*     */   
/*     */   public LocalDateTime getLoginTime() {
/*  74 */     return this.loginTime;
/*     */   }
/*     */ 
/*     */   
/*     */   public String getSessionDuration() {
/*  79 */     if (this.loginTime == null) {
/*  80 */       return "0 minute";
/*     */     }
/*     */     
/*     */     try {
/*  84 */       Duration duration = Duration.between(this.loginTime, LocalDateTime.now());
/*  85 */       long minutes = duration.toMinutes();
/*  86 */       long hours = duration.toHours();
/*  87 */       minutes %= 60L;
/*     */       
/*  89 */       if (hours > 0L) {
/*  90 */         return "" + hours + "h " + hours + "m";
/*     */       }
/*  92 */       return "" + minutes + " minute" + minutes;
/*     */     }
/*  94 */     catch (Exception e) {
/*  95 */       return "Erreur calcul durée";
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean isSessionExpired() {
/* 101 */     if (this.loginTime == null) return true;
/*     */ 
/*     */     
/*     */     try {
/* 105 */       LocalDateTime expiryTime = this.loginTime.plusHours(8L);
/* 106 */       return LocalDateTime.now().isAfter(expiryTime);
/* 107 */     } catch (Exception e) {
/* 108 */       return true;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public String getRemainingTime() {
/* 114 */     if (this.loginTime == null || !this.isLoggedIn) return "Session inactive";
/*     */     
/*     */     try {
/* 117 */       LocalDateTime expiryTime = this.loginTime.plusHours(8L);
/* 118 */       Duration remaining = Duration.between(LocalDateTime.now(), expiryTime);
/*     */       
/* 120 */       if (remaining.isNegative()) {
/* 121 */         return "Session expirée";
/*     */       }
/*     */       
/* 124 */       long hours = remaining.toHours();
/* 125 */       long minutes = remaining.toMinutes() % 60L;
/*     */       
/* 127 */       return "" + hours + "h " + hours + "m";
/* 128 */     } catch (Exception e) {
/* 129 */       return "Erreur calcul";
/*     */     } 
/*     */   }
/*     */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myap\\util\SessionManager.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */