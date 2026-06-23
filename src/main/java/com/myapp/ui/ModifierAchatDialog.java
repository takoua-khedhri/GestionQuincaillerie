package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifierAchatDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(ModifierAchatDialog.class);
    
    private JComboBox<String> comboStatut;
    private int factureId;
    private HistoriqueAchats parent;
    
    public ModifierAchatDialog(int factureId, HistoriqueAchats parent) {
        this.factureId = factureId;
        this.parent = parent;
        
        setTitle("Modifier le statut de la facture");
        setModal(true);
        setSize(400, 200);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.add(new JLabel("Statut:"));
        comboStatut = new JComboBox<>();
        comboStatut.addItem("EN_ATTENTE");
        comboStatut.addItem("PAYEE");
        comboStatut.addItem("ANNULEE");
        formPanel.add(comboStatut);
        
        // Charger le statut actuel
        String sql = "SELECT statut FROM FacturesAchat WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            var rs = pst.executeQuery();
            if (rs.next()) {
                String statutActuel = rs.getString("statut");
                comboStatut.setSelectedItem(statutActuel);
            }
        } catch (Exception e) {
            log.error("Error loading current status for factureId={}", factureId, e);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnEnregistrer = new JButton("Enregistrer");
        JButton btnAnnuler = new JButton("Annuler");
        
        btnEnregistrer.addActionListener(e -> enregistrer());
        btnAnnuler.addActionListener(e -> dispose());
        
        buttonPanel.add(btnEnregistrer);
        buttonPanel.add(btnAnnuler);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void enregistrer() {
        String nouveauStatut = (String) comboStatut.getSelectedItem();
        
        String sql = "UPDATE FacturesAchat SET statut = ? WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, factureId);
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "✅ Statut mis à jour avec succès !");
                
                // Rafraîchir le tableau parent
               
                
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Aucune modification effectuée", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Error updating status for factureId={}", factureId, e);
            JOptionPane.showMessageDialog(this, "Erreur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}