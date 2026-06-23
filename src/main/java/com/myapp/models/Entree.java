/*    */ package com.myapp.models;
/*    */ 
/*    */ import java.time.LocalDateTime;
/*    */ 
/*    */ public class Entree {
/*    */   private int id;
/*    */   private int articleId;
/*    */   private String designation;
/*    */   private int quantite;
/*    */   private LocalDateTime dateEntree;
/*    */   private String utilisateur;
/*    */   
/*    */   public Entree() {}
/*    */   
/*    */   public Entree(int articleId, String designation, int quantite, String utilisateur) {
/* 16 */     this.articleId = articleId;
/* 17 */     this.designation = designation;
/* 18 */     this.quantite = quantite;
/* 19 */     this.utilisateur = utilisateur;
/* 20 */     this.dateEntree = LocalDateTime.now();
/*    */   }
/*    */   
/*    */   public int getId() {
/* 24 */     return this.id; } public void setId(int id) {
/* 25 */     this.id = id;
/*    */   }
/* 27 */   public int getArticleId() { return this.articleId; } public void setArticleId(int articleId) {
/* 28 */     this.articleId = articleId;
/*    */   }
/* 30 */   public String getDesignation() { return this.designation; } public void setDesignation(String designation) {
/* 31 */     this.designation = designation;
/*    */   }
/* 33 */   public int getQuantite() { return this.quantite; } public void setQuantite(int quantite) {
/* 34 */     this.quantite = quantite;
/*    */   }
/* 36 */   public LocalDateTime getDateEntree() { return this.dateEntree; } public void setDateEntree(LocalDateTime dateEntree) {
/* 37 */     this.dateEntree = dateEntree;
/*    */   }
/* 39 */   public String getUtilisateur() { return this.utilisateur; } public void setUtilisateur(String utilisateur) {
/* 40 */     this.utilisateur = utilisateur;
/*    */   }
/*    */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myapp\models\Entree.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */