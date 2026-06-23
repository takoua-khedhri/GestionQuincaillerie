/* Decompiler 41ms, total 818ms, lines 106 */
package com.myapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GestionSocieteUI extends JFrame {
   private static final Logger log = LoggerFactory.getLogger(GestionSocieteUI.class);
   private JTextField txtNomSociete;
   private JTextField txtAdresse;
   private JTextField txtTelephone;
   private JTextField txtEmail;
   private JTextField txtSIRET;
   private JTextField txtTVA;

   public GestionSocieteUI() {
      this.setTitle("Gestion des Données Société");
      this.setExtendedState(6);
      this.setDefaultCloseOperation(2);
      this.setLocationRelativeTo((Component)null);
      this.setLayout(new BorderLayout(10, 10));
      JPanel headerPanel = new JPanel();
      headerPanel.setBackground(new Color(0, 100, 200));
      headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
      JLabel titleLabel = new JLabel("INFORMATIONS SOCIÉTÉ");
      titleLabel.setFont(new Font("Arial", 1, 18));
      titleLabel.setForeground(Color.WHITE);
      headerPanel.add(titleLabel);
      this.add(headerPanel, "North");
      JPanel formPanel = new JPanel(new GridBagLayout());
      formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(5, 5, 5, 5);
      gbc.fill = 2;
      this.txtNomSociete = new JTextField(20);
      this.txtAdresse = new JTextField(20);
      this.txtTelephone = new JTextField(20);
      this.txtEmail = new JTextField(20);
      this.txtSIRET = new JTextField(20);
      this.txtTVA = new JTextField(20);
      this.chargerDonneesSociete();
      int row = 0;
      int var9 = row + 1;
      this.addFormField(formPanel, gbc, "Nom Société:", this.txtNomSociete, row);
      this.addFormField(formPanel, gbc, "Adresse:", this.txtAdresse, var9++);
      this.addFormField(formPanel, gbc, "Téléphone:", this.txtTelephone, var9++);
      this.addFormField(formPanel, gbc, "Email:", this.txtEmail, var9++);
      this.addFormField(formPanel, gbc, "SIRET:", this.txtSIRET, var9++);
      this.addFormField(formPanel, gbc, "N° TVA:", this.txtTVA, var9++);
      this.add(formPanel, "Center");
      JPanel buttonPanel = new JPanel(new FlowLayout(2));
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
      JButton btnSauvegarder = new JButton("Sauvegarder");
      btnSauvegarder.setBackground(new Color(0, 150, 0));
      btnSauvegarder.setForeground(Color.WHITE);
      JButton btnAnnuler = new JButton("Annuler");
      btnAnnuler.setBackground(new Color(200, 0, 0));
      btnAnnuler.setForeground(Color.WHITE);
      buttonPanel.add(btnAnnuler);
      buttonPanel.add(btnSauvegarder);
      this.add(buttonPanel, "South");
      btnSauvegarder.addActionListener((e) -> {
         this.sauvegarderDonnees();
      });
      btnAnnuler.addActionListener((e) -> {
         this.dispose();
      });
   }

   private void addFormField(JPanel panel, GridBagConstraints gbc, String label, JTextField field, int row) {
      gbc.gridx = 0;
      gbc.gridy = row;
      gbc.weightx = 0.0D;
      panel.add(new JLabel(label), gbc);
      gbc.gridx = 1;
      gbc.gridy = row;
      gbc.weightx = 1.0D;
      panel.add(field, gbc);
   }

   private void chargerDonneesSociete() {
      this.txtNomSociete.setText("MA SOCIÉTÉ SARL");
      this.txtAdresse.setText("123 Avenue des Champs-Élysées, 75008 Paris");
      this.txtTelephone.setText("01 23 45 67 89");
      this.txtEmail.setText("contact@masociete.fr");
      this.txtSIRET.setText("123 456 789 00012");
      this.txtTVA.setText("FR 12 123456789");
   }

   private void sauvegarderDonnees() {
      JOptionPane.showMessageDialog(this, "Données société sauvegardées avec succès!\n\nCes informations apparaîtront sur vos factures.", "Sauvegarde réussie", 1);
      this.dispose();
   }
}
