package com.myapp.logic;

import com.myapp.db.DatabaseManager;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class DevisManager {
    private DefaultTableModel model;
    private DecimalFormat df;
    private boolean modePrixGros = true;

    public DevisManager(DefaultTableModel model) {
        this.model = model;
        this.df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        System.out.println("✅ DevisManager initialisé");
    }

    public void setModePrixGros(boolean modePrixGros) {
        this.modePrixGros = modePrixGros;
    }

    public boolean isModePrixGros() {
        return modePrixGros;
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
    }

    // =====================================================================
    // AJOUTER ARTICLE - avec prix de base pour référence
    // Colonnes : 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA, 6=PU TTC, 7=Remise %, 8=Total TTC
    // =====================================================================
    public void ajouterArticle(String designation, int quantiteAjout, double prixTTC, double remisePourcentage) {
        try {
            int tauxTVA = DatabaseManager.getTauxTVAArticle(designation);
            if (tauxTVA == 0) tauxTVA = 19;
            String reference = DatabaseManager.getReferenceArticle(designation);

            if (prixTTC == 0.0D) {
                throw new Exception("Prix non trouvé pour l'article: " + designation);
            }

            // PU HT calculé automatiquement depuis PU TTC
            double puHT = prixTTC / (1.0 + tauxTVA / 100.0);

            // Vérifier si l'article existe déjà dans le tableau
            for (int i = 0; i < this.model.getRowCount(); i++) {
                String articleDansTableau = this.model.getValueAt(i, 1).toString();

                if (articleDansTableau.equals(designation)) {
                    int qteActuelle = Integer.parseInt(this.model.getValueAt(i, 2).toString());
                    int nouvelleQte = qteActuelle + quantiteAjout;

                    // Lire la remise existante (colonne 7)
                    String remiseStr = this.model.getValueAt(i, 7).toString()
                            .replace("%", "").replace(",", ".").trim();
                    double remiseExistante = 0.0;
                    try {
                        remiseExistante = Double.parseDouble(remiseStr);
                    } catch (NumberFormatException e) {
                        remiseExistante = 0.0;
                    }

                    // Recalcul des totaux avec la remise existante
                    double totalTTCBrut = prixTTC * nouvelleQte;
                    double totalHTBrut = puHT * nouvelleQte;
                    double totalTTCFinal = totalTTCBrut * (1.0 - remiseExistante / 100.0);
                    double totalHTFinal = totalHTBrut * (1.0 - remiseExistante / 100.0);

                    this.model.setValueAt(String.valueOf(nouvelleQte), i, 2);
                    this.model.setValueAt(formatMontantDinar(puHT), i, 3);           // PU HT
                    this.model.setValueAt(formatMontantDinar(totalHTFinal), i, 4);    // Total HT
                    this.model.setValueAt(formatMontantDinar(totalTTCFinal), i, 8);   // Total TTC

                    System.out.println("🔄 Quantité incrémentée pour: " + designation);
                    return;
                }
            }

            // Calcul pour nouvel article avec remise
            double totalTTCBrut = prixTTC * quantiteAjout;
            double totalHTBrut = puHT * quantiteAjout;
            double totalTTCFinal = totalTTCBrut * (1.0 - remisePourcentage / 100.0);
            double totalHTFinal = totalHTBrut * (1.0 - remisePourcentage / 100.0);

            Object[] ligne = {
                reference,                                          // 0: Code
                designation,                                        // 1: Désignation
                quantiteAjout,                                      // 2: Qté
                formatMontantDinar(puHT),                          // 3: PU HT
                formatMontantDinar(totalHTFinal),                  // 4: Total HT (après remise)
                tauxTVA + " %",                                    // 5: TVA
                formatMontantDinar(prixTTC),                       // 6: PU TTC
                formaterPourcentage(remisePourcentage),            // 7: Remise %
                formatMontantDinar(totalTTCFinal)                  // 8: Total TTC (après remise)
            };

            this.model.addRow(ligne);
            System.out.println("✅ Nouvel article ajouté: " + designation);

        } catch (Exception e) {
            System.err.println("❌ Erreur dans DevisManager: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // CALCUL TOTAUX - avec prise en compte de la remise par ligne
    // =====================================================================
    public CalculTotaux calculerTotaux() {
        double totalHT = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;
        double totalRemiseLignes = 0.0;

        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                String puTTCStr = this.model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String qteStr = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                String totalTTCStr = this.model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String tvaStr = this.model.getValueAt(i, 5).toString().replaceAll("[^0-9]", "");
                String designation = this.model.getValueAt(i, 1).toString();

                double puTTC = Double.parseDouble(puTTCStr);
                int quantite = Integer.parseInt(qteStr);
                double ttcLigne = Double.parseDouble(totalTTCStr);
                int tvaPct = Integer.parseInt(tvaStr);

                // PRIX DE BASE depuis la BDD pour calculer la remise en DT
                double prixTTCBase = modePrixGros
                    ? DatabaseManager.getPrixGrosArticle(designation)
                    : DatabaseManager.getPrixDetailArticle(designation);
                
                // TTC brut sans remise = Prix de base × Qté
                double ttcBrut = prixTTCBase * quantite;
                
                // Remise ligne en DT = TTC brut - TTC final
                double remiseLigneDT = ttcBrut - ttcLigne;
                if (remiseLigneDT < 0) remiseLigneDT = 0;

                // HT final = TTC final / (1 + tva)
                double htFinal = ttcLigne / (1.0 + tvaPct / 100.0);
                double tvaFinal = ttcLigne - htFinal;

                totalHT += htFinal;
                totalTVA += tvaFinal;
                totalTTC += ttcLigne;
                totalRemiseLignes += remiseLigneDT;

            } catch (Exception e) {
                System.err.println("Erreur calcul ligne " + i + ": " + e.getMessage());
            }
        }

        return new CalculTotaux(totalHT, totalTVA, totalTTC, totalRemiseLignes);
    }

    public boolean validerDevis(String numero, String date, String client) {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Le devis est vide.", "Attention", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        System.out.println("✅ Devis validé");
        return true;
    }

    public void viderTableau() {
        this.model.setRowCount(0);
    }

    private String formatMontantDinar(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3local = new DecimalFormat("#,##0.000", symbols);
        String formatted = df3local.format(montant);
        formatted = formatted.replace(",", ".");
        if (formatted.endsWith(".000")) {
            return formatted.replace(".", ",");
        }
        return formatted;
    }

    public static class CalculTotaux {
        public final double totalHT;
        public final double totalTVA;
        public final double totalTTC;
        public final double montantRemise;

        public CalculTotaux(double totalHT, double totalTVA, double totalTTC, double montantRemise) {
            this.totalHT = totalHT;
            this.totalTVA = totalTVA;
            this.totalTTC = totalTTC;
            this.montantRemise = montantRemise;
        }
    }
}