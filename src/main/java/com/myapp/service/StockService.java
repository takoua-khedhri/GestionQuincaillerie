/*    */ package com.myapp.service;
/*    */ 
/*    */ import com.myapp.db.ConnexionSQLite;
/*    */ import java.sql.Connection;
/*    */ import java.sql.PreparedStatement;
/*    */ import java.sql.ResultSet;
/*    */ import javax.swing.table.DefaultTableModel;
/*    */ 
/*    */ public class StockService {
/*    */   public static boolean verifierStock(String designation, int quantiteDemandee) {
/*    */     
/* 12 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 13 */       try { PreparedStatement pst = conn.prepareStatement("SELECT stock FROM Articles WHERE designation = ?");
/*    */         
/* 15 */         try { pst.setString(1, designation);
/* 16 */           ResultSet rs = pst.executeQuery();
/*    */           
/* 18 */           if (rs.next())
/* 19 */           { int stockActuel = rs.getInt("stock");
/* 20 */             boolean bool = (stockActuel >= quantiteDemandee) ? true : false;
/*    */             
/* 22 */             if (pst != null) pst.close();  if (conn != null) conn.close();  return bool; }  if (pst != null) pst.close();  } catch (Throwable throwable) { if (pst != null) try { pst.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  if (conn != null) conn.close();  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 23 */     { System.out.println("Erreur vérification stock: " + e.getMessage()); }
/*    */     
/* 25 */     return false;
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public static int getStockActuel(String designation) {
/*    */     
/* 32 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 33 */       try { PreparedStatement pst = conn.prepareStatement("SELECT stock FROM Articles WHERE designation = ?");
/*    */         
/* 35 */         try { pst.setString(1, designation);
/* 36 */           ResultSet rs = pst.executeQuery();
/*    */           
/* 38 */           if (rs.next())
/* 39 */           { int i = rs.getInt("stock");
/*    */             
/* 41 */             if (pst != null) pst.close();  if (conn != null) conn.close();  return i; }  if (pst != null) pst.close();  } catch (Throwable throwable) { if (pst != null) try { pst.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  if (conn != null) conn.close();  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 42 */     { System.out.println("Erreur récupération stock: " + e.getMessage()); }
/*    */     
/* 44 */     return 0;
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public static boolean mettreAJourStock(String designation, int quantiteVendue) {
/*    */     
/* 51 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 52 */       try { PreparedStatement pst = conn.prepareStatement("UPDATE Articles SET stock = stock - ? WHERE designation = ?");
/*    */         
/* 54 */         try { pst.setInt(1, quantiteVendue);
/* 55 */           pst.setString(2, designation);
/*    */           
/* 57 */           int rowsAffected = pst.executeUpdate();
/* 58 */           boolean bool = (rowsAffected > 0) ? true : false;
/*    */           
/* 60 */           if (pst != null) pst.close();  if (conn != null) conn.close();  return bool; } catch (Throwable throwable) { if (pst != null) try { pst.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 61 */     { System.out.println("Erreur mise à jour stock: " + e.getMessage());
/*    */       
/* 63 */       return false; }
/*    */   
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public static boolean verifierStockFacture(DefaultTableModel model) {
/* 70 */     for (int i = 0; i < model.getRowCount(); i++) {
/* 71 */       String designation = model.getValueAt(i, 1).toString();
/* 72 */       int quantite = Integer.parseInt(model.getValueAt(i, 2).toString());
/*    */       
/* 74 */       if (!verifierStock(designation, quantite)) {
/* 75 */         return false;
/*    */       }
/*    */     } 
/* 78 */     return true;
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public static boolean mettreAJourStockFacture(DefaultTableModel model) {
/* 85 */     boolean succesTotal = true;
/*    */     
/* 87 */     for (int i = 0; i < model.getRowCount(); i++) {
/* 88 */       String designation = model.getValueAt(i, 1).toString();
/* 89 */       int quantite = Integer.parseInt(model.getValueAt(i, 2).toString());
/*    */       
/* 91 */       if (!mettreAJourStock(designation, quantite)) {
/* 92 */         succesTotal = false;
/*    */       }
/*    */     } 
/* 95 */     return succesTotal;
/*    */   }
/*    */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myapp\service\StockService.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */