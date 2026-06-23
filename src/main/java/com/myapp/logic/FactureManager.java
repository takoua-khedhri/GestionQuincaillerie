package com.myapp.logic;

import com.myapp.db.DatabaseManager;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class FactureManager {

    private DefaultTableModel model;
    private DecimalFormat df;
    private DecimalFormat df3;
    private double timbre = 1.0D;
    private boolean modePrixGros = true;

    public FactureManager(DefaultTableModel model) {
        this.model = model;
        this.df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        this.df3 = new DecimalFormat("#,##0.000", new DecimalFormatSymbols(Locale.FRANCE));
        System.out.println("✅ FactureManager initialisé");
    }

    public void setModePrixGros(boolean modePrixGros) {
        this.modePrixGros = modePrixGros;
    }

    public boolean isModePrixGros() {
        return modePrixGros;
    }

    public double getTimbre() {
        return this.timbre;
    }

    public void setTimbre(double nouveauTimbre) {
        if (nouveauTimbre >= 0.0D) {
            this.timbre = nouveauTimbre;
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
    // AJOUTER ARTICLE - avec prix de base pour référence
    // Colonnes : 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA, 6=PU TTC, 7=Remise %, 8=Total TTC
    // =====================================================================
    public void ajouterArticle(String designation, int quantiteAjout, double prixTTC, double remisePourcentage) {
        try {
            int stockActuel = DatabaseManager.getStockActuel(designation);
            String reference = DatabaseManager.getReferenceArticle(designation);
            int tauxTVA = DatabaseManager.getTauxTVAArticle(designation);
            if (tauxTVA == 0) tauxTVA = 19;

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
                                "Déjà dans la facture : " + qteActuelle + "\n" +
                                "Total requis : " + nouvelleQte);
                    }

                    this.model.setValueAt(nouvelleQte, i, 2);

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

            // Vérification stock pour nouvel article
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
                quantiteAjout,                                      // 2: Qté
                formatMontantDinar(puHT),                          // 3: PU HT
                formatMontantDinar(totalHTFinal),                  // 4: Total HT (après remise)
                tauxTVA + " %",                                    // 5: TVA
                formatMontantDinar(prixTTC),                       // 6: PU TTC
                formaterPourcentage(remisePourcentage),            // 7: Remise %
                formatMontantDinar(totalTTCFinal)                  // 8: Total TTC (après remise)
            };

            this.model.addRow(ligne);
            System.out.println("✅ Nouvel article ajouté: " + designation + " | Prix TTC: " + prixTTC + " | Remise: " + remisePourcentage + "%");

        } catch (Exception e) {
            System.err.println("❌ Erreur ajouterArticle: " + e.getMessage());
            JOptionPane.showMessageDialog(null, e.getMessage(), "Erreur Stock / Saisie", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    // AJOUTER ARTICLE SANS REMISE (pour compatibilité)
    // =====================================================================
    public void ajouterArticle(String designation, int quantite, double prix) {
        ajouterArticle(designation, quantite, prix, 0.0);
    }

    // =====================================================================
    // CALCUL TOTAUX - avec prise en compte de la remise par ligne
    // Affichage du total remise en DT en bas
    // =====================================================================
    public CalculTotaux calculerTotaux(double timbreActuel, double remiseGlobalePourcentage) {
        double totalHTGlobal = 0.0;
        double totalTVAGlobal = 0.0;
        double totalTTCLignes = 0.0;
        double totalRemiseLignes = 0.0;  // Total remise en DINARS

        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                // Lecture des colonnes
                String designation = this.model.getValueAt(i, 1).toString();
                String qteStr = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                String remiseStr = this.model.getValueAt(i, 7).toString().replace("%", "").replace(",", ".").trim();
                String tvaStr = this.model.getValueAt(i, 5).toString().replaceAll("[^0-9]", "");
                String totalTTCStr = this.model.getValueAt(i, 8).toString().replace(",", ".").replaceAll("[^0-9.-]", "");

                int quantite = Integer.parseInt(qteStr);
                double remisePct = remiseStr.isEmpty() ? 0.0 : Double.parseDouble(remiseStr);
                int tvaPct = Integer.parseInt(tvaStr);
                double ttcLigne = Double.parseDouble(totalTTCStr);

                // PRIX DE BASE depuis la BDD pour calculer la remise en DT
                double prixTTCBase = modePrixGros
                    ? DatabaseManager.getPrixGrosArticle(designation)
                    : DatabaseManager.getPrixDetailArticle(designation);
                
                // TTC brut sans remise = Prix de base × Qté
                double ttcBrut = prixTTCBase * quantite;
                
                // Remise en DT = TTC brut - TTC final (après remise)
                double remiseLigneDT = ttcBrut - ttcLigne;
                if (remiseLigneDT < 0) remiseLigneDT = 0;

                // HT final = TTC final / (1 + TVA/100)
                double htFinal = ttcLigne / (1.0 + tvaPct / 100.0);
                
                // TVA finale = TTC final - HT final
                double tvaFinal = ttcLigne - htFinal;

                totalHTGlobal += htFinal;
                totalTVAGlobal += tvaFinal;
                totalTTCLignes += ttcLigne;
                totalRemiseLignes += remiseLigneDT;  // Accumulation de la remise en DT

            } catch (Exception e) {
                System.err.println("Erreur calcul ligne " + i + ": " + e.getMessage());
            }
        }

        double totalTTCGlobal = totalTTCLignes + timbreActuel;

        return new CalculTotaux(
            totalHTGlobal,       // Total HT (après remise)
            totalTVAGlobal,      // Total TVA
            totalTTCGlobal,      // Total TTC avec timbre
            totalRemiseLignes,   // Total REMISE en DINARS (affiché en bas)
            0.0,                 // retenueSource (gérée par FactureUI)
            totalTTCGlobal       // netAPayer avant retenue
        );
    }

    // =====================================================================
    // CALCUL TOTAUX SANS REMISE GLOBALE (version simplifiée)
    // =====================================================================
    public CalculTotaux calculerTotaux() {
        return calculerTotaux(this.timbre, 0.0);
    }

    // =====================================================================
    // VALIDATION FACTURE
    // =====================================================================
    public boolean validerFacture(String numeroFacture, String dateFacture, String clientNom) {
        if (this.model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "La facture est vide.", "Attention", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // Vérification du stock pour tous les articles
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
        
        System.out.println("✅ Facture validée - Stock vérifié");
        return true;
    }

    public void viderTableau() {
        this.model.setRowCount(0);
    }

    public int getNombreArticles() {
        return this.model.getRowCount();
    }

    public DefaultTableModel getModel() {
        return model;
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

    // =====================================================================
    // MÉTHODE POUR METTRE À JOUR LE STOCK APRÈS FACTURE
    // =====================================================================
    public void mettreAJourStockApresFacture() {
        for (int i = 0; i < this.model.getRowCount(); i++) {
            try {
                String designation = this.model.getValueAt(i, 1).toString();
                String qteStr = this.model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "");
                int quantite = Integer.parseInt(qteStr);
                DatabaseManager.mettreAJourStock(designation, quantite);
            } catch (Exception e) {
                System.err.println("Erreur mise à jour stock: " + e.getMessage());
            }
        }
    }

    // =====================================================================
    // CLASSE INTERNE CalculTotaux
    // =====================================================================
    public static class CalculTotaux {
        public final double totalHT;
        public final double totalTVA;
        public final double totalTTC;
        public final double totalRemise;   // somme des remises par ligne en DT
        public final double retenueSource;
        public final double netAPayer;

        public CalculTotaux(double totalHT, double totalTVA, double totalTTC,
                            double totalRemise, double retenueSource, double netAPayer) {
            this.totalHT = totalHT;
            this.totalTVA = totalTVA;
            this.totalTTC = totalTTC;
            this.totalRemise = totalRemise;
            this.retenueSource = retenueSource;
            this.netAPayer = netAPayer;
        }

        @Override
        public String toString() {
            return String.format(
                "Totaux: HT=%.3f, TVA=%.3f, TTC=%.3f, Remise=%.3f, Retenue=%.3f, Net=%.3f",
                totalHT, totalTVA, totalTTC, totalRemise, retenueSource, netAPayer
            );
        }
    }
}