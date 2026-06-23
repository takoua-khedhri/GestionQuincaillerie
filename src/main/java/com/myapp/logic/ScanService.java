package com.myapp.logic;

import com.myapp.models.Article;
import com.myapp.db.DatabaseManager;
import java.awt.Component;
import java.awt.Toolkit;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class ScanService {

    // =========================================================================
    // 1. VERSION POUR LA FACTURE (FactureUI)
    // =========================================================================
    public boolean traiterScan(String code, DefaultTableModel model, FactureManager manager, Component parent) {
        Article article = validerEtRecupererArticle(code, parent);
        if (article == null) return false;

        int rowIndex = trouverLigneArticle(model, article.getDesignation());

        if (rowIndex != -1) {
            return incrementerQuantite(model, rowIndex, article.getStock(), parent);
        } else {
            // CORRECTION : prix récupéré et passé en paramètre, remise = 0.0
            double prixScan = DatabaseManager.getPrixGrosArticle(article.getDesignation());
            manager.ajouterArticle(article.getDesignation(), 1, prixScan, 0.0);
            return true;
        }
    }

    // =========================================================================
    // 2. VERSION POUR LE BON DE LIVRAISON (BLUI)
    // =========================================================================
    public boolean traiterScan(String code, DefaultTableModel model, BLManager manager, Component parent) {
        Article article = validerEtRecupererArticle(code, parent);
        if (article == null) return false;

        int rowIndex = trouverLigneArticle(model, article.getDesignation());

        if (rowIndex != -1) {
            return incrementerQuantite(model, rowIndex, article.getStock(), parent);
        } else {
            // CORRECTION : prix récupéré et passé en paramètre, remise = 0.0
            double prixScan = DatabaseManager.getPrixGrosArticle(article.getDesignation());
            manager.ajouterArticle(article.getDesignation(), 1, prixScan, 0.0);
            return true;
        }
    }

    // =========================================================================
    // 3. VERSION POUR LE BON DE SORTIE (BonSortieUI)
    // =========================================================================
    public boolean traiterScan(String code, DefaultTableModel model, BonSortieManager manager, Component parent) {
        Article article = validerEtRecupererArticle(code, parent);
        if (article == null) return false;

        int rowIndex = trouverLigneArticle(model, article.getDesignation());

        if (rowIndex != -1) {
            return incrementerQuantite(model, rowIndex, article.getStock(), parent);
        } else {
            // CORRECTION : prix récupéré et passé en paramètre, remise = 0.0
            double prixScan = DatabaseManager.getPrixGrosArticle(article.getDesignation());
            manager.ajouterArticle(article.getDesignation(), 1, prixScan, 0.0);
            return true;
        }
    }

    // =========================================================================
    // 4. VERSION POUR LE DEVIS (DevisUI)
    // =========================================================================
    public boolean traiterScan(String code, DefaultTableModel model, DevisManager manager, Component parent) {
        Article article = validerEtRecupererArticle(code, parent);
        if (article == null) return false;

        int rowIndex = trouverLigneArticle(model, article.getDesignation());

        if (rowIndex != -1) {
            return incrementerQuantite(model, rowIndex, article.getStock(), parent);
        } else {
            // CORRECTION : prix récupéré et passé en paramètre, remise = 0.0
            double prixScan = DatabaseManager.getPrixGrosArticle(article.getDesignation());
            manager.ajouterArticle(article.getDesignation(), 1, prixScan, 0.0);
            return true;
        }
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES PRIVÉES (LOGIQUE COMMUNE)
    // =========================================================================

    /**
     * Vérifie le code en BDD, l'existence de l'article et le stock disponible.
     * @return L'objet Article si tout est OK, sinon null.
     */
    private Article validerEtRecupererArticle(String code, Component parent) {
        if (code == null || code.trim().isEmpty()) return null;

        // 1. Recherche BDD
        Article article = DatabaseManager.getArticleByCode(code.trim());

        if (article == null) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Code barre inconnu : " + code, "Article non trouvé", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        // 2. Vérification Stock Global
        if (article.getStock() <= 0) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Cet article est en rupture de stock !", "Stock Épuisé", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return article;
    }

    /**
     * Cherche l'index de la ligne contenant l'article dans le tableau.
     * Suppose que la colonne "Désignation" est toujours à l'index 1.
     */
    private int trouverLigneArticle(DefaultTableModel model, String designationCherchee) {
        for (int i = 0; i < model.getRowCount(); i++) {
            String designationLigne = model.getValueAt(i, 1).toString();
            if (designationLigne.equals(designationCherchee)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tente d'augmenter la quantité de la ligne de 1, en vérifiant le stock max.
     * Suppose que la colonne "Quantité" est toujours à l'index 2.
     */
    private boolean incrementerQuantite(DefaultTableModel model, int rowIndex, int stockMax, Component parent) {
        try {
            int qteActuelle = Integer.parseInt(model.getValueAt(rowIndex, 2).toString());
            int nouvelleQte = qteActuelle + 1;

            // Vérification stock
            if (nouvelleQte > stockMax) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(parent, "Stock insuffisant ! (Max: " + stockMax + ")", "Stock", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Mise à jour (Le Listener du JTable recalculera les prix automatiquement)
            model.setValueAt(String.valueOf(nouvelleQte), rowIndex, 2);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }
}