/*    */ package com.myapp.models;
/*    */ 
/*    */ import java.time.LocalDateTime;
/*    */ 
/*    */ public class Sortie {
/*    */   private int id;
/*    */   private int articleId;
/*    */   private String designation;
/*    */   private int quantite;
/*    */   private LocalDateTime dateSortie;
/*    */   private String utilisateur;
/*    */   private String motif;
/*    */   
/*    */   public Sortie() {}
/*    */   
/*    */   public Sortie(int articleId, String designation, int quantite, String utilisateur, String motif) {
/* 17 */     this.articleId = articleId;
/* 18 */     this.designation = designation;
/* 19 */     this.quantite = quantite;
/* 20 */     this.utilisateur = utilisateur;
/* 21 */     this.motif = motif;
/* 22 */     this.dateSortie = LocalDateTime.now();
/*    */   }
/*    */   
/*    */   public int getId() {
/* 26 */     return this.id; } public void setId(int id) {
/* 27 */     this.id = id;
/*    */   }
/* 29 */   public int getArticleId() { return this.articleId; } public void setArticleId(int articleId) {
/* 30 */     this.articleId = articleId;
/*    */   }
/* 32 */   public String getDesignation() { return this.designation; } public void setDesignation(String designation) {
/* 33 */     this.designation = designation;
/*    */   }
/* 35 */   public int getQuantite() { return this.quantite; } public void setQuantite(int quantite) {
/* 36 */     this.quantite = quantite;
/*    */   }
/* 38 */   public LocalDateTime getDateSortie() { return this.dateSortie; } public void setDateSortie(LocalDateTime dateSortie) {
/* 39 */     this.dateSortie = dateSortie;
/*    */   }
/* 41 */   public String getUtilisateur() { return this.utilisateur; } public void setUtilisateur(String utilisateur) {
/* 42 */     this.utilisateur = utilisateur;
/*    */   }
/* 44 */   public String getMotif() { return this.motif; } public void setMotif(String motif) {
/* 45 */     this.motif = motif;
/*    */   }
/*    */ }


/* Location:              C:\libapp\LibrairieApp.jar!\com\myapp\models\Sortie.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */