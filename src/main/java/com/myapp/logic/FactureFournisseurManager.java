package com.myapp.logic;

import com.myapp.db.DatabaseManager;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.table.DefaultTableModel;

public class FactureFournisseurManager {

    private DefaultTableModel model;
    private DecimalFormat df;

    public FactureFournisseurManager(DefaultTableModel model) {
        this.model = model;
        this.df = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
    }

    /**
     * Ajoute un article à la table.
     * Si l'article existe déjà, fusionne les quantités.
     */
    public void ajouterArticle(String designation, int quantite, double prixUnitaireHT) {
        try {
            String code = DatabaseManager.getReferenceArticle(designation);
            if (code == null || code.trim().isEmpty()) code = "—";

            int tva = 19;  // TVA fixe à 19%

            // Fusion si article déjà présent
            for (int i = 0; i < model.getRowCount(); i++) {
                String desigExistante = model.getValueAt(i, 1).toString();
                if (desigExistante.equalsIgnoreCase(designation.trim())) {
                    int    qteActuelle  = parseQte(model.getValueAt(i, 2).toString());
                    int    nouvelleQte  = qteActuelle + quantite;
                    double puExistant   = parseMontant(model.getValueAt(i, 3).toString());
                    double newHT        = nouvelleQte * puExistant;
                    double newTTC       = newHT * (1.0 + tva / 100.0);
                    model.setValueAt(nouvelleQte, i, 2);
                    model.setValueAt(formatMontant(newHT), i, 4);
                    model.setValueAt(formatMontant(newTTC), i, 6);
                    return;
                }
            }

            double totalHT  = quantite * prixUnitaireHT;
            double totalTTC = totalHT * (1.0 + tva / 100.0);

            Object[] ligne = {
                code,
                designation.trim(),
                quantite,
                formatMontant(prixUnitaireHT),
                formatMontant(totalHT),
                tva,  // Stocker comme nombre
                formatMontant(totalTTC)
            };
            model.addRow(ligne);

        } catch (Exception e) {
            System.err.println("❌ Erreur ajouterArticle : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Recalcule la ligne (row) après modification de la quantité dans la table.
     */
    public void recalculerLigne(int row) {
        try {
            int qte = parseQte(model.getValueAt(row, 2).toString());
            if (qte <= 0) qte = 1;
            model.setValueAt(qte, row, 2);

            double pu = parseMontant(model.getValueAt(row, 3).toString());
            
            // Récupérer la TVA correctement
            Object tvaObj = model.getValueAt(row, 5);
            double tva = 19.0;
            if (tvaObj instanceof Number) {
                tva = ((Number) tvaObj).doubleValue();
            } else if (tvaObj instanceof String) {
                String tvaStr = tvaObj.toString().replaceAll("[^0-9]", "");
                if (!tvaStr.isEmpty()) tva = Double.parseDouble(tvaStr);
            }

            double ht = pu * qte;
            double ttc = ht * (1.0 + tva / 100.0);

            model.setValueAt(formatMontant(ht), row, 4);
            model.setValueAt(formatMontant(ttc), row, 6);

        } catch (Exception e) {
            System.err.println("❌ Erreur recalculerLigne " + row + " : " + e.getMessage());
        }
    }

    /**
     * Calcule les totaux HT, TVA et TTC depuis toutes les lignes.
     */
    public CalculTotauxAchat calculerTotaux() {
        double totalHT = 0;
        double totalTVA = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                double ht = parseMontant(model.getValueAt(i, 4).toString());
                totalHT += ht;

                // Récupérer la TVA correctement
                Object tvaObj = model.getValueAt(i, 5);
                double tauxTVA = 19.0;
                if (tvaObj instanceof Number) {
                    tauxTVA = ((Number) tvaObj).doubleValue();
                } else if (tvaObj instanceof String) {
                    String tvaStr = tvaObj.toString().replaceAll("[^0-9]", "");
                    if (!tvaStr.isEmpty()) tauxTVA = Double.parseDouble(tvaStr);
                }
                
                totalTVA += ht * (tauxTVA / 100.0);

            } catch (Exception e) {
                System.err.println("⚠️ Erreur calcul ligne " + i + " : " + e.getMessage());
            }
        }

        return new CalculTotauxAchat(totalHT, totalTVA);
    }

    // Formatage
    public String formatMontant(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        return df3.format(montant);
    }

    // Utilitaires de parsing
    private int parseQte(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private double parseMontant(String s) {
        try {
            String clean = s.replaceAll("\\s", "")
                            .replace(",", ".")
                            .replaceAll("[^0-9.-]", "");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Classe résultat
    public static class CalculTotauxAchat {
        public final double totalHT;
        public final double totalTVA;
        public final double totalTTC;

        public CalculTotauxAchat(double totalHT, double totalTVA) {
            this.totalHT = totalHT;
            this.totalTVA = totalTVA;
            this.totalTTC = totalHT + totalTVA;
        }
    }
}