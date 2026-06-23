/*    */ package com.myapp.db;
/*    */ import java.sql.Connection;
/*    */ import java.sql.ResultSet;
/*    */ import java.sql.Statement;
/*    */ 
/*    */ public class InitDB {
/*    */   public static void createTablesIfNeeded() {
/*  8 */     String sqlArticles = "CREATE TABLE IF NOT EXISTS Articles (id INTEGER PRIMARY KEY AUTOINCREMENT, designation TEXT NOT NULL, prix REAL NOT NULL, stock INTEGER NOT NULL);";
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 15 */     String sqlFactures = "CREATE TABLE IF NOT EXISTS Factures (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, date TEXT NOT NULL, client_id INTEGER, FOREIGN KEY(client_id) REFERENCES Clients(id));";
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 23 */     String sqlDetails = "CREATE TABLE IF NOT EXISTS DetailsFacture (id INTEGER PRIMARY KEY AUTOINCREMENT, facture_id INTEGER, article_id INTEGER, quantite INTEGER, prix_unitaire REAL, FOREIGN KEY(facture_id) REFERENCES Factures(id), FOREIGN KEY(article_id) REFERENCES Articles(id));";
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 33 */     String sqlClients = "CREATE TABLE IF NOT EXISTS Clients (id INTEGER PRIMARY KEY AUTOINCREMENT, nom TEXT NOT NULL);";
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 38 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 39 */       try { Statement st = conn.createStatement(); 
/* 40 */         try { st.execute(sqlArticles);
/* 41 */           st.execute(sqlClients);
/* 42 */           st.execute(sqlFactures);
/* 43 */           st.execute(sqlDetails);
/* 44 */           System.out.println("Toutes les tables créées (si nécessaire).");
/* 45 */           if (st != null) st.close();  } catch (Throwable throwable) { if (st != null) try { st.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  if (conn != null) conn.close();  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 46 */     { e.printStackTrace(); }
/*    */   
/*    */   }
/*    */   public static void insertSampleArticlesIfEmpty() {
/*    */     
/* 51 */     try { Connection conn = ConnexionSQLite.getConnection(); 
/* 52 */       try { Statement st = conn.createStatement(); 
/* 53 */         try { ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM Articles");
/* 54 */           if (rs.next() && rs.getInt("c") == 0) {
/* 55 */             st.executeUpdate("INSERT INTO Articles (designation,prix,stock) VALUES ('Huile moteur', 15.5, 10), ('Filtre à huile', 8.0, 20), ('Balai essuie-glace', 12.0, 15)");
/*    */ 
/*    */ 
/*    */             
/* 59 */             System.out.println("Articles exemples insérés.");
/*    */           } 
/* 61 */           if (st != null) st.close();  } catch (Throwable throwable) { if (st != null) try { st.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  if (conn != null) conn.close();  } catch (Throwable throwable) { if (conn != null) try { conn.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 62 */     { e.printStackTrace(); }
/*    */   
/*    */   }
/*    */   
/*    */   public static void main(String[] args) {
/* 67 */     createTablesIfNeeded();
/* 68 */     insertSampleArticlesIfEmpty();
/*    */   }
/*    */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myapp\db\InitDB.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */