/*    */ package com.myapp.db;
/*    */ 
/*    */ import java.sql.Connection;
/*    */ import java.sql.DriverManager;
/*    */ import java.sql.SQLException;
/*    */ 
/*    */ public class TestSQLite {
/*    */   public static void main(String[] args) {
/*    */     try {
/* 10 */       Connection conn = DriverManager.getConnection("jdbc:sqlite:librairie.db");
/* 11 */       System.out.println("Connexion réussie !");
/* 12 */       conn.close();
/* 13 */     } catch (SQLException e) {
/* 14 */       System.out.println("Erreur de connexion : " + e.getMessage());
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myapp\db\TestSQLite.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */