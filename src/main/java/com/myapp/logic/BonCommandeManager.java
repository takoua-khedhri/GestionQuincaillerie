package com.myapp.logic;

import com.myapp.db.DatabaseManager;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class BonCommandeManager {
    private DefaultTableModel model;
    private DecimalFormat df;
    private DecimalFormat df3;
    
    public BonCommandeManager(DefaultTableModel model) {
        this.model = model;
        this.df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        this.df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
        System.out.println("✅ BonCommandeManager initialisé");
    }
    
    // ==================== MÉTHODE DE FORMATAGE POURCENTAGE ====================
    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
    }
    // =========================================================================
    
    public void ajouterArticle(String designation, int quantite, double prixAchat) {
        try {
            // Vérifier si l'article est déjà dans le tableau
            for (int i = 0; i < this.model.getRowCount(); i++) {
                String articleDansTableau = this.model.getValueAt(i, 1).toString();
                
                if (articleDansTableau.equals(designation)) {
                    // Incrémenter la quantité
                    int qteActuelle = Integer.parseInt(this.model.getValueAt(i, 2).toString());
                    int nouvelleQte = qteActuelle + quantite;
                    
                    // Récupérer la remise existante
                    String remiseStr = this.model.getValueAt(i, 4).toString()
                            .replace("%", "").replace(",", ".").trim();
                    double remiseExistante = 0.0;
                    try {
                        remiseExistante = Double.parseDouble(remiseStr);
                    } catch (NumberFormatException e) {
                        remiseExistante = 0.0;
                    }
                    
                    // Recalculer le total HT
                    double totalBrut = prixAchat * nouvelleQte;
                    double montantRemiseLigne = totalBrut * (remiseExistante / 100.0);
                    double totalHT = totalBrut - montantRemiseLigne;
                    
                    // Mettre à jour
                    this.model.setValueAt(nouvelleQte, i, 2);
                    this.model.setValueAt(formatMontantDinar(totalHT), i, 5);
                    
                    System.out.println("🔄 Quantité incrémentée pour " + designation);
                    return;
                }
            }
            
            // Nouvel article
            String reference = DatabaseManager.getReferenceArticle(designation);
            double totalHT = prixAchat * quantite;
            
            Object[] ligne = {
                reference,
                designation,
                quantite,
                formatMontantDinar(prixAchat),
                formaterPourcentage(0.0),
                formatMontantDinar(totalHT)
            };
            
            this.model.addRow(ligne);
            System.out.println("✅ Article ajouté: " + designation);
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Calcule les totaux en lisant directement les colonnes affichées dans le tableau.
     * 
     * MONTANT REMISE = somme des remises par ligne en DT uniquement
     * (plus de remise globale)
     * 
     * Remise ligne en DT = Total HT brut (PU × Qte) - Total HT affiché (col 5)
     * 
     * @return Objet CalculTotaux contenant les totaux calculés
     */
    public CalculTotaux calculerTotaux() {
        double totalHT           = 0.0;
        double totalRemiseLignes = 0.0; // somme des remises par ligne en DT

        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                // Lire le PU Achat (col 3) et la Quantité (col 2)
                String puStr    = getValeurTableau(i, 3);
                String qteStr   = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                String totalHTStr = getValeurTableau(i, 5);
                
                double puAchat   = Double.parseDouble(puStr);
                int    quantite  = Integer.parseInt(qteStr.isEmpty() ? "0" : qteStr);
                double htLigne   = Double.parseDouble(totalHTStr); // Total HT affiché (col 5, avec remise ligne)
                
                // Total HT brut sans remise ligne
                double htBrut = puAchat * quantite;
                
                // Remise ligne en DT = HT brut - HT affiché
                double remiseLigneDT = htBrut - htLigne;
                if (remiseLigneDT < 0) remiseLigneDT = 0;
                
                totalHT           += htLigne;
                totalRemiseLignes += remiseLigneDT;
                
            } catch (Exception e) {
                System.err.println("Erreur calcul ligne " + i + ": " + e.getMessage());
            }
        }
        
        // Plus de remise globale — montantRemise = uniquement remises lignes en DT
        // Pour le bon de commande, TTC = HT (pas de TVA)
        return new CalculTotaux(totalHT, totalHT, totalRemiseLignes);
    }
    
    private String getValeurTableau(int row, int column) {
        try {
            Object value = this.model.getValueAt(row, column);
            if (value == null) return "0";
            String strValue = value.toString().replace(",", ".").replaceAll("[^0-9.-]", "");
            return strValue.isEmpty() ? "0" : strValue;
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        
        String formatted = df3.format(montant);
        formatted = formatted.replace(",", ".");
        
        if (formatted.endsWith(".000")) {
            return formatted.replace(".", ",");
        }
        
        return formatted;
    }
    
    public boolean validerBonCommande(String numero, String date, String fournisseur) {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Le bon de commande est vide.");
            return false;
        }
        
        if (fournisseur == null || fournisseur.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Veuillez sélectionner un fournisseur.");
            return false;
        }
        
        System.out.println("✅ Bon de Commande validé");
        return true;
    }
    
    public void viderTableau() {
        this.model.setRowCount(0);
    }
    
    public static class CalculTotaux {
        public final double totalHT;
        public final double totalTTC;
        public final double montantRemise;
        
        public CalculTotaux(double totalHT, double totalTTC, double montantRemise) {
            this.totalHT = totalHT;
            this.totalTTC = totalTTC;
            this.montantRemise = montantRemise;
        }
    }
}