package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetailsAchatDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(DetailsAchatDialog.class);
    
    private DecimalFormat df;
    
    public DetailsAchatDialog(int factureId, String numero, String fournisseur) {
        df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRANCE));
        
        setTitle("Détails de la facture - " + numero);
        setModal(true);
        setSize(800, 500);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Informations générales
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBorder(new javax.swing.border.TitledBorder("Informations générales"));
        
        infoPanel.add(new JLabel("N° Facture:"));
        infoPanel.add(new JLabel(numero));
        infoPanel.add(new JLabel("Fournisseur:"));
        infoPanel.add(new JLabel(fournisseur));
        
        // Récupérer date et montants
        String sql = "SELECT date_facture, total_ht, tva, total_ttc, retenue_source_pourcentage, net_a_payer, statut " +
                    "FROM FacturesAchat WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                infoPanel.add(new JLabel("Date:"));
                infoPanel.add(new JLabel(rs.getString("date_facture")));
                infoPanel.add(new JLabel("Total HT:"));
                infoPanel.add(new JLabel(formatMontant(rs.getDouble("total_ht"))));
                infoPanel.add(new JLabel("TVA:"));
                infoPanel.add(new JLabel(formatMontant(rs.getDouble("tva"))));
                infoPanel.add(new JLabel("Total TTC:"));
                infoPanel.add(new JLabel(formatMontant(rs.getDouble("total_ttc"))));
                infoPanel.add(new JLabel("Retenue source:"));
                infoPanel.add(new JLabel(rs.getDouble("retenue_source_pourcentage") + "%"));
                infoPanel.add(new JLabel("Net à payer:"));
                infoPanel.add(new JLabel(formatMontant(rs.getDouble("net_a_payer"))));
                infoPanel.add(new JLabel("Statut:"));
                infoPanel.add(new JLabel(rs.getString("statut")));
            }
        } catch (Exception e) {
            log.error("Error loading achat details for factureId={}", factureId, e);
        }
        
        // Table des articles
        String[] columns = {"Désignation", "Quantité", "PU HT", "Total HT", "TVA", "Total TTC"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        String sqlDetails = "SELECT article_designation, quantite, prix_unitaire_ht, total_ht, tva_pourcentage FROM DetailsAchat WHERE facture_achat_id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlDetails)) {
            pst.setInt(1, factureId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                double pu = rs.getDouble("prix_unitaire_ht");
                double totalHT = rs.getDouble("total_ht");
                int tvaPct = rs.getInt("tva_pourcentage");
                double totalTTC = totalHT * (1 + tvaPct / 100.0);
                
                Object[] row = {
                    rs.getString("article_designation"),
                    rs.getInt("quantite"),
                    formatMontant(pu),
                    formatMontant(totalHT),
                    tvaPct + "%",
                    formatMontant(totalTTC)
                };
                model.addRow(row);
            }
        } catch (Exception e) {
            log.error("Error loading achat details for factureId={}", factureId, e);
        }
        
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(table);
        
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private String formatMontant(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.000", symbols);
        return df.format(montant).replace(",", ".");
    }
}