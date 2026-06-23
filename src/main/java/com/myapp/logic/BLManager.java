package com.myapp.logic;

import com.myapp.db.DatabaseManager;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class BLManager {
    private DefaultTableModel model;
    private DecimalFormat df;
    private boolean modePrixGros = true;

    public BLManager(DefaultTableModel model) {
        this.model = model;
        this.df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        System.out.println("✅ BLManager initialisé");
    }

    public void setModePrixGros(boolean modePrixGros) {
        this.modePrixGros = modePrixGros;
    }

    public boolean isModePrixGros() {
        return modePrixGros;
    }

    // =====================================================================
    // AJOUTER ARTICLE - avec prix de base pour référence
    // Colonnes : 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA, 6=PU TTC, 7=Remise %, 8=Total TTC
    // =====================================================================
    public void ajouterArticle(String designation, int quantiteAjout, double prixTTC, double remisePourcentage) {
        System.out.println("🔄 BLManager.ajouterArticle() : " + designation);
        try {
            int stockActuel = DatabaseManager.getStockActuel(designation);
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

                    if (nouvelleQte > stockActuel) {
                        throw new Exception("Stock insuffisant pour '" + designation + "' !\n" +
                                "Stock disponible : " + stockActuel + "\n" +
                                "Déjà dans le BL : " + qteActuelle + "\n" +
                                "Total requis : " + nouvelleQte);
                    }

                    this.model.setValueAt(String.valueOf(nouvelleQte), i, 2);

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

                    this.model.setValueAt(formatMontantDinar(puHT), i, 3);           // PU HT
                    this.model.setValueAt(formatMontantDinar(totalHTFinal), i, 4);    // Total HT
                    this.model.setValueAt(formatMontantDinar(totalTTCFinal), i, 8);   // Total TTC

                    System.out.println("🔄 Quantité incrémentée pour: " + designation);
                    return;
                }
            }

            if (quantiteAjout > stockActuel) {
                throw new Exception("Stock insuffisant pour '" + designation + "' !\n" +
                        "Stock disponible : " + stockActuel + "\n" +
                        "Quantité demandée : " + quantiteAjout);
            }

            // Calcul pour nouvel article avec remise
            double totalTTCBrut = prixTTC * quantiteAjout;
            double totalHTBrut = puHT * quantiteAjout;
            double totalTTCFinal = totalTTCBrut * (1.0 - remisePourcentage / 100.0);
            double totalHTFinal = totalHTBrut * (1.0 - remisePourcentage / 100.0);

            Object[] ligne = {
                reference,                                          // 0: Code
                designation,                                        // 1: Désignation
                String.valueOf(quantiteAjout),                      // 2: Qté
                formatMontantDinar(puHT),                          // 3: PU HT
                formatMontantDinar(totalHTFinal),                  // 4: Total HT
                tauxTVA + " %",                                    // 5: TVA
                formatMontantDinar(prixTTC),                       // 6: PU TTC
                formaterPourcentage(remisePourcentage),            // 7: Remise %
                formatMontantDinar(totalTTCFinal)                  // 8: Total TTC
            };

            this.model.addRow(ligne);
            System.out.println("✅ Nouvel article ajouté: " + designation);

        } catch (Exception e) {
            System.err.println("❌ Erreur dans BLManager: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erreur Stock / Saisie", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formaterPourcentage(double valeur) {
        if (valeur == (int) valeur) {
            return (int) valeur + " %";
        } else {
            return df.format(valeur) + " %";
        }
    }

    // =====================================================================
    // CALCUL TOTAUX - avec prise en compte de la remise par ligne
    // =====================================================================
    public CalculTotaux calculerTotaux() {
        double totalHT = 0.0;
        double totalTVA = 0.0;
        double totalTTC = 0.0;
        double totalRemise = 0.0;

        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                String puTTCStr = this.model.getValueAt(i, 6).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String qteStr = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                String tvaStr = this.model.getValueAt(i, 5).toString().replaceAll("[^0-9]", "");
                String totalTTCStr = this.model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");
                String designation = this.model.getValueAt(i, 1).toString();

                double puTTC = Double.parseDouble(puTTCStr);
                int quantite = Integer.parseInt(qteStr);
                int tvaPct = Integer.parseInt(tvaStr);
                double ttcLigne = Double.parseDouble(totalTTCStr);

                // PRIX DE BASE depuis la BDD pour calculer la remise en DT
                double prixTTCBase = modePrixGros
                    ? DatabaseManager.getPrixGrosArticle(designation)
                    : DatabaseManager.getPrixDetailArticle(designation);
                
                // TTC brut sans remise = Prix de base × Qté
                double ttcBrut = prixTTCBase * quantite;
                
                // Remise en DT = TTC brut - TTC final (après remise)
                double montantRemiseLigne = ttcBrut - ttcLigne;
                if (montantRemiseLigne < 0) montantRemiseLigne = 0;

                // HT final = TTC final / (1 + tva)
                double htFinal = ttcLigne / (1.0 + tvaPct / 100.0);
                double tvaFinal = ttcLigne - htFinal;

                totalHT += htFinal;
                totalTVA += tvaFinal;
                totalTTC += ttcLigne;
                totalRemise += montantRemiseLigne;

            } catch (Exception e) {
                System.err.println("Erreur parsing ligne " + i + ": " + e.getMessage());
            }
        }

        return new CalculTotaux(totalHT, totalTVA, totalTTC, totalRemise);
    }

    public boolean validerBL(String numeroBL, String dateBL, String clientNom) {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Le BL est vide.", "Attention", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (clientNom == null || clientNom.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Veuillez sélectionner ou saisir un client.", "Attention", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                String designation = this.model.getValueAt(i, 1).toString();
                String qteStr = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                int quantite = Integer.parseInt(qteStr);

                if (!DatabaseManager.verifierStock(designation, quantite)) {
                    int stockActuel = DatabaseManager.getStockActuel(designation);
                    JOptionPane.showMessageDialog(null,
                        "❌ Action Bloquée : Stock insuffisant!\n\nArticle: " + designation +
                        "\nQuantité demandée: " + quantite +
                        "\nStock disponible: " + stockActuel,
                        "Erreur Stock", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        System.out.println("✅ BL validé - Stock vérifié");
        return true;
    }

    public void mettreAJourStock(String designation, int quantite) {
        DatabaseManager.mettreAJourStock(designation, quantite);
        System.out.println("📦 Stock mis à jour pour: " + designation + " - Quantité: " + quantite);
    }

    public void viderTableau() {
        this.model.setRowCount(0);
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