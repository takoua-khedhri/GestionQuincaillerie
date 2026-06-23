/*     */ package com.myapp.util;
/*     */ import com.myapp.db.ConnexionSQLite;
/*     */ import java.sql.Connection;
/*     */ import java.sql.PreparedStatement;
/*     */ import java.sql.ResultSet;
/*     */ import java.time.LocalDateTime;
/*     */ import java.time.temporal.ChronoUnit;
/*     */ 
/*     */ public class DemoManager {
/*     */   private static final int DEMO_DURATION_DAYS = 7;
/*     */   private static final long MINUTES_PER_DAY = 1440L;
/*     */   private static final long DEMO_DURATION_MINUTES = 10080L;
/*     */   
/*     */   public static LocalDateTime getDemoStartTime() {
/*     */     
/*  16 */     try { Connection conn = ConnexionSQLite.getConnection();
/*     */       
/*  18 */       try { String sql = "SELECT start_time FROM demo WHERE id = 1";
/*  19 */         PreparedStatement ps = conn.prepareStatement(sql);
/*  20 */         ResultSet rs = ps.executeQuery();
/*     */         
/*  22 */         if (rs.next())
/*  23 */         { LocalDateTime localDateTime = LocalDateTime.parse(rs.getString("start_time"));
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/*  35 */           if (conn != null) conn.close();  return localDateTime; }  LocalDateTime now = LocalDateTime.now(); String insertSql = "INSERT INTO demo (id, start_time, expired) VALUES (1, ?, 0)"; PreparedStatement psInsert = conn.prepareStatement(insertSql); psInsert.setString(1, now.toString()); psInsert.executeUpdate(); LocalDateTime localDateTime1 = now; if (conn != null) conn.close();  return localDateTime1; } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/*  36 */     { return LocalDateTime.now(); }
/*     */   
/*     */   }
/*     */   
/*     */   public static boolean isDemoExpired() {
/*     */     
/*  42 */     try { Connection conn = ConnexionSQLite.getConnection();
/*     */ 
/*     */       
/*  45 */       try { String checkExpiredSql = "SELECT expired FROM demo WHERE id = 1";
/*  46 */         PreparedStatement psCheck = conn.prepareStatement(checkExpiredSql);
/*  47 */         ResultSet rsCheck = psCheck.executeQuery();
/*     */         
/*  49 */         if (rsCheck.next() && rsCheck.getInt("expired") == 1)
/*  50 */         { boolean bool1 = true;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/*  68 */           if (conn != null) conn.close();  return bool1; }  LocalDateTime start = getDemoStartTime(); LocalDateTime now = LocalDateTime.now(); long minutesElapsed = ChronoUnit.MINUTES.between(start, now); if (minutesElapsed >= 10080L) { String updateSql = "UPDATE demo SET expired = 1 WHERE id = 1"; PreparedStatement psUpdate = conn.prepareStatement(updateSql); psUpdate.executeUpdate(); boolean bool1 = true; if (conn != null) conn.close();  return bool1; }  boolean bool = false; if (conn != null) conn.close();  return bool; } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/*  69 */     { return false; }
/*     */   
/*     */   }
/*     */ 
/*     */   
/*     */   public static long getRemainingMinutes() {
/*     */     try {
/*  76 */       LocalDateTime start = getDemoStartTime();
/*  77 */       LocalDateTime now = LocalDateTime.now();
/*     */       
/*  79 */       long elapsedMinutes = ChronoUnit.MINUTES.between(start, now);
/*  80 */       long remainingMinutes = 10080L - elapsedMinutes;
/*     */       
/*  82 */       return Math.max(0L, remainingMinutes);
/*  83 */     } catch (Exception e) {
/*  84 */       return 0L;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public static long getRemainingDays() {
/*     */     try {
/*  91 */       LocalDateTime start = getDemoStartTime();
/*  92 */       LocalDateTime now = LocalDateTime.now();
/*     */ 
/*     */       
/*  95 */       long daysElapsed = ChronoUnit.DAYS.between(start, now);
/*  96 */       long remainingDays = 7L - daysElapsed;
/*     */ 
/*     */       
/*  99 */       if (remainingDays < 0L) remainingDays = 0L; 
/* 100 */       if (remainingDays > 7L) remainingDays = 7L;
/*     */       
/* 102 */       return remainingDays;
/*     */     }
/* 104 */     catch (Exception e) {
/* 105 */       return 7L;
/*     */     } 
/*     */   }
/*     */   
/*     */   public static void resetDemo() {
/*     */     
/* 111 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 112 */       try { String sql = "DELETE FROM demo WHERE id = 1";
/* 113 */         PreparedStatement ps = conn.prepareStatement(sql);
/* 114 */         ps.executeUpdate();
/* 115 */         if (conn != null) conn.close();  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 116 */     { e.printStackTrace(); }
/*     */   
/*     */   }
/*     */ 
/*     */   
/*     */   public static long getElapsedDays() {
/*     */     try {
/* 123 */       LocalDateTime start = getDemoStartTime();
/* 124 */       LocalDateTime now = LocalDateTime.now();
/* 125 */       long elapsedDays = ChronoUnit.DAYS.between(start, now);
/* 126 */       return Math.min(elapsedDays, 7L);
/* 127 */     } catch (Exception e) {
/* 128 */       return 0L;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public static int getDemoDurationDays() {
/* 134 */     return 7;
/*     */   }
/*     */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myap\\util\DemoManager.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */